/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.mapping.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.OneToMany;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.ToOne;
import org.hibernate.mapping.Value;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingBuildContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingConfigurationCollector;
import org.hibernate.search.mapper.orm.model.impl.HibernateOrmBootstrapIntrospector;
import org.hibernate.search.mapper.pojo.extractor.mapping.programmatic.ContainerExtractorPath;
import org.hibernate.search.mapper.pojo.extractor.builtin.BuiltinContainerExtractors;
import org.hibernate.search.mapper.pojo.mapping.building.spi.ErrorCollectingPojoTypeMetadataContributor;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoTypeMetadataContributor;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingConfigurationContributor;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPath;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPathValueNode;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;

public final class HibernateOrmMetatadaContributor implements PojoMappingConfigurationContributor {
	private final HibernateOrmBootstrapIntrospector introspector;
	private final Map<String, PersistentClass> persistentClasses;

	public HibernateOrmMetatadaContributor(HibernateOrmBootstrapIntrospector introspector,
			Map<String, PersistentClass> persistentClasses) {
		this.introspector = introspector;
		this.persistentClasses = persistentClasses;
	}

	@Override
	public void configure(MappingBuildContext buildContext,
			MappingConfigurationCollector<PojoTypeMetadataContributor> configurationCollector) {
		PropertyDelegatesCollector delegatesCollector = new PropertyDelegatesCollector();

		// Ensure all entities are declared as such and have their inverse associations declared
		for ( PersistentClass persistentClass : persistentClasses.values() ) {
			Class<?> clazz = persistentClass.getMappedClass();
			PojoRawTypeModel<?> typeModel = introspector.getTypeModel( clazz );
			collectPropertyDelegates( delegatesCollector, clazz, persistentClass.getPropertyIterator() );

			String identifierPropertyName = persistentClass.getIdentifierProperty().getName();
			List<PojoTypeMetadataContributor> delegates = delegatesCollector.buildAndRemove( clazz );

			configurationCollector.collectContributor(
					typeModel,
					new ErrorCollectingPojoTypeMetadataContributor()
							.add( new HibernateOrmEntityTypeMetadataContributor(
									persistentClass, identifierPropertyName
							) )
							.addAll( delegates )
			);
		}

		for ( Map.Entry<Class<?>, List<PojoTypeMetadataContributor>> entry :
				delegatesCollector.buildRemaining().entrySet() ) {
			PojoRawTypeModel<?> typeModel = introspector.getTypeModel( entry.getKey() );
			List<PojoTypeMetadataContributor> delegates = entry.getValue();
			if ( !delegates.isEmpty() ) {
				configurationCollector.collectContributor(
						typeModel,
						new ErrorCollectingPojoTypeMetadataContributor()
								.addAll( delegates )
				);
			}
		}
	}

	@SuppressWarnings( "rawtypes" ) // Hibernate ORM gives us raw types, we must make do.
	private void collectPropertyDelegates(PropertyDelegatesCollector collector,
			Class<?> javaClass, Iterator propertyIterator) {
		collector.markAsSeen( javaClass );

		// Sort the properties before processing for deterministic iteration
		List<Property> properties = new ArrayList<>();
		while ( propertyIterator.hasNext() ) {
			properties.add( (Property) propertyIterator.next() );
		}
		properties.sort( Comparator.comparing( Property::getName ) );

		for ( Property property : properties ) {
			collectPropertyMetadataContributors( collector, javaClass, property );
		}
	}

	private void collectPropertyMetadataContributors(PropertyDelegatesCollector collector,
			Class<?> javaClass, Property property) {
		Value value = property.getValue();
		if ( value instanceof org.hibernate.mapping.Collection ) {
			org.hibernate.mapping.Collection collectionValue = (org.hibernate.mapping.Collection) value;
			/*
			 * Note about Maps:
			 * Associations hosted on Map keys cannot have an inverse side in Hibernate ORM metadata,
			 * so we only care about the Map values, which happen to be represented by Map.getElement()
			 * in the Hibernate ORM metadata.
			 */
			Value element = collectionValue.getElement();
			String referencedEntityName = getReferencedEntityName( element );
			String mappedByPath = collectionValue.getMappedByProperty();
			if ( referencedEntityName != null && mappedByPath != null && !mappedByPath.isEmpty() ) {
				collector.collect(
						javaClass,
						new HibernateOrmAssociationInverseSideMetadataContributor(
								property.getName(), getExtractorPath( collectionValue ),
								resolveMappedByPath( referencedEntityName, mappedByPath )
						)
				);
			}
		}
		else if ( value instanceof ToOne ) {
			ToOne toOneValue = (ToOne) value;
			String referencedEntityName = toOneValue.getReferencedEntityName();
			// For *ToOne, the "mappedBy" information is apparently stored in the ReferencedPropertyName
			String mappedByPath = toOneValue.getReferencedPropertyName();
			if ( mappedByPath != null && !mappedByPath.isEmpty() ) {
				collector.collect(
						javaClass,
						new HibernateOrmAssociationInverseSideMetadataContributor(
								property.getName(), getExtractorPath( toOneValue ),
								resolveMappedByPath( referencedEntityName, mappedByPath )
						)
				);
			}
		}
		else if ( value instanceof Component ) {
			collector.collect(
					javaClass,
					new HibernateOrmAssociationEmbeddedMetadataContributor(
							property.getName(), getExtractorPath( value )
					)
			);
			Component componentValue = (Component) value;
			Class<?> componentClass = componentValue.getComponentClass();
			/*
			 * Different Component instances for the same component class may carry different metadata
			 * depending on where they appear,
			 * because Hibernate ORM allows overriding using @AssociationOverride/@AttributeOverride
			 * on an embedded property.
			 * But Hibernate ORM does not allow overriding "entity-level" metadata, only "table-level" metadata.
			 * For instance overriding the name of a property is not allowed, nor is adding another embedded association.
			 * As a result, all the components should behave similarly in our case, since we are only interested in
			 * "entity-level" metadata.
			 * Thus we only use the first Component instance we find, and ignore the others.
			 */
			if ( !collector.hasSeen( componentClass ) ) {
				collectPropertyDelegates( collector, componentClass, componentValue.getPropertyIterator() );
			}
		}
		else if ( value instanceof SimpleValue ) {
			collectScaleContributor( collector, javaClass, property, value );
		}
	}

	private void collectScaleContributor(PropertyDelegatesCollector collector, Class<?> javaClass, Property property, Value value) {
		Iterator<Selectable> ci = value.getColumnIterator();
		while ( ci.hasNext() ) {
			Selectable selectable = ci.next();
			if ( selectable instanceof Column ) {
				int scale = ( (Column) selectable ).getScale();
				HibernateOrmJpaColumnScaleContributor scaleContributor = new HibernateOrmJpaColumnScaleContributor(
						property.getName(), getExtractorPath( value ), scale );
				collector.collect( javaClass, scaleContributor );
			}
		}
	}

	private String getReferencedEntityName(Value element) {
		if ( element instanceof OneToMany ) {
			return ( (OneToMany) element ).getReferencedEntityName();
		}
		else if ( element instanceof ToOne ) {
			return ( (ToOne) element ).getReferencedEntityName();
		}
		else {
			return null;
		}
	}

	private PojoModelPathValueNode resolveMappedByPath(String inverseSideEntity, String mappedByPath) {
		StringTokenizer tokenizer = new StringTokenizer( mappedByPath, ".", false );

		String rootPropertyName = tokenizer.nextToken();
		PojoModelPath.Builder inverseSidePathBuilder = PojoModelPath.builder().property( rootPropertyName );
		Property property = persistentClasses.get( inverseSideEntity ).getProperty( rootPropertyName );

		do {
			Value value = property.getValue();
			inverseSidePathBuilder.value( getExtractorPath( value ) );

			if ( tokenizer.hasMoreTokens() ) {
				Component component = (Component) value;
				String propertyName = tokenizer.nextToken();
				property = component.getProperty( propertyName );
				inverseSidePathBuilder.property( propertyName );
			}
			else {
				property = null;
			}
		}
		while ( property != null );

		return inverseSidePathBuilder.toValuePath();
	}

	private ContainerExtractorPath getExtractorPath(Value value) {
		if ( value instanceof org.hibernate.mapping.Collection ) {
			org.hibernate.mapping.Collection collectionValue = (org.hibernate.mapping.Collection) value;
			String extractorName = getExtractorName( collectionValue );
			return ContainerExtractorPath.explicitExtractor( extractorName );
		}
		else {
			return ContainerExtractorPath.noExtractors();
		}
	}

	private String getExtractorName(org.hibernate.mapping.Collection collectionValue) {
		if ( collectionValue instanceof org.hibernate.mapping.Array ) {
			// Caution if you add other if ( ... instanceof ) branches: Array extends List!
			return BuiltinContainerExtractors.ARRAY;
		}
		else if ( collectionValue instanceof org.hibernate.mapping.Map ) {
			// See contributeModelPropertyNode(), we only care about map values, not about keys
			return BuiltinContainerExtractors.MAP_VALUE;
		}
		else {
			return BuiltinContainerExtractors.COLLECTION;
		}
	}

	private static class PropertyDelegatesCollector {
		// Use a LinkedHashMap for deterministic iteration
		private final Map<Class<?>, List<PojoTypeMetadataContributor>> result = new LinkedHashMap<>();

		public void markAsSeen(Class<?> clazz) {
			getList( clazz );
		}

		public boolean hasSeen(Class<?> clazz) {
			return result.containsKey( clazz );
		}

		public void collect(Class<?> clazz, PojoTypeMetadataContributor contributor) {
			getList( clazz ).add( contributor );
		}

		public List<PojoTypeMetadataContributor> buildAndRemove(Class<?> clazz) {
			List<PojoTypeMetadataContributor> built = getList( clazz );
			result.remove( clazz );
			return built;
		}

		public Map<Class<?>, List<PojoTypeMetadataContributor>> buildRemaining() {
			return result;
		}

		private List<PojoTypeMetadataContributor> getList(Class<?> clazz) {
			return result.computeIfAbsent( clazz, ignored -> new ArrayList<>() );
		}
	}
}
