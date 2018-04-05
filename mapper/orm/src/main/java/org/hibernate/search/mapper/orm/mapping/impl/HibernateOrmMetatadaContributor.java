/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.mapping.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import org.hibernate.boot.Metadata;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.OneToMany;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.ToOne;
import org.hibernate.mapping.Value;
import org.hibernate.search.engine.common.spi.BuildContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.MapperFactory;
import org.hibernate.search.engine.mapper.mapping.building.spi.MetadataCollector;
import org.hibernate.search.engine.mapper.mapping.building.spi.MetadataContributor;
import org.hibernate.search.mapper.orm.model.impl.HibernateOrmBootstrapIntrospector;
import org.hibernate.search.mapper.pojo.extractor.ContainerValueExtractor;
import org.hibernate.search.mapper.pojo.extractor.ContainerValueExtractorPath;
import org.hibernate.search.mapper.pojo.extractor.builtin.ArrayElementExtractor;
import org.hibernate.search.mapper.pojo.extractor.builtin.CollectionElementExtractor;
import org.hibernate.search.mapper.pojo.extractor.builtin.MapValueExtractor;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoTypeMetadataContributor;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPath;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPathPropertyNode;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPathValueNode;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;

public final class HibernateOrmMetatadaContributor implements MetadataContributor {
	private final MapperFactory<PojoTypeMetadataContributor, ?> mapperFactory;
	private final HibernateOrmBootstrapIntrospector introspector;
	private final Metadata metadata;

	public HibernateOrmMetatadaContributor(MapperFactory<PojoTypeMetadataContributor, ?> mapperFactory,
			HibernateOrmBootstrapIntrospector introspector, Metadata metadata) {
		this.mapperFactory = mapperFactory;
		this.introspector = introspector;
		this.metadata = metadata;
	}

	@Override
	public void contribute(BuildContext buildContext, MetadataCollector collector) {
		// Ensure all entities are declared as such and have their inverse associations declared
		for ( PersistentClass persistentClass : metadata.getEntityBindings() ) {
			Class<?> clazz = persistentClass.getMappedClass();
			// getMappedClass() can return null, which should be ignored
			if ( clazz != null ) {
				PojoRawTypeModel<?> typeModel = introspector.getTypeModel( clazz );
				collector.collectContributor(
						mapperFactory, typeModel,
						new HibernateOrmEntityTypeMetadataContributor(
								createPropertyDelegates( persistentClass.getPropertyIterator() )
						)
				);
			}
		}
	}

	@SuppressWarnings( "rawtypes" ) // Hibernate ORM gives us raw types, we must make do.
	private List<PojoTypeMetadataContributor> createPropertyDelegates(Iterator propertyIterator) {
		List<PojoTypeMetadataContributor> delegates = new ArrayList<>();
		while ( propertyIterator.hasNext() ) {
			Property property = (Property) propertyIterator.next();
			collectPropertyMetadataContributors( delegates, property );
		}
		return delegates;
	}

	private void collectPropertyMetadataContributors(List<PojoTypeMetadataContributor> collector, Property property) {
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
			String referencedEntityName = null;
			if ( element instanceof OneToMany ) {
				referencedEntityName = ( (OneToMany) element ).getReferencedEntityName();
			}
			else if ( element instanceof ToOne ) {
				referencedEntityName = ( (ToOne) element ).getReferencedEntityName();
			}
			if ( referencedEntityName != null ) {
				String mappedByPath = collectionValue.getMappedByProperty();
				if ( mappedByPath != null && !mappedByPath.isEmpty() ) {
					collector.add( new HibernateOrmAssociationInverseSideMetadataContributor(
							property.getName(), getExtractorPath( collectionValue ),
							resolveMappedByPath( referencedEntityName, mappedByPath )
					) );
				}
			}
		}
		else if ( value instanceof ToOne ) {
			ToOne toOneValue = (ToOne) value;
			String referencedEntityName = toOneValue.getReferencedEntityName();
			// For *ToOne, the "mappedBy" information is apparently stored in the ReferencedPropertyName
			String mappedByPath = toOneValue.getReferencedPropertyName();
			if ( mappedByPath != null && !mappedByPath.isEmpty() ) {
				collector.add( new HibernateOrmAssociationInverseSideMetadataContributor(
						property.getName(), getExtractorPath( toOneValue ),
						resolveMappedByPath( referencedEntityName, mappedByPath )
				) );
			}
		}
	}

	private PojoModelPathValueNode resolveMappedByPath(String inverseSideEntity, String mappedByPath) {
		StringTokenizer tokenizer = new StringTokenizer( mappedByPath, ".", false );

		String rootPropertyName = tokenizer.nextToken();
		PojoModelPathPropertyNode inverseSidePropertyPath = PojoModelPath.fromRoot( rootPropertyName );
		PojoModelPathValueNode inverseSideValuePath;
		Property property = metadata.getEntityBinding( inverseSideEntity ).getProperty( rootPropertyName );

		do {
			Value value = property.getValue();
			inverseSideValuePath = inverseSidePropertyPath.value( getExtractorPath( value ) );

			if ( tokenizer.hasMoreTokens() ) {
				Component component = (Component) value;
				String propertyName = tokenizer.nextToken();
				property = component.getProperty( propertyName );
				inverseSidePropertyPath = inverseSideValuePath.property( propertyName );
			}
			else {
				property = null;
			}
		}
		while ( property != null );

		return inverseSideValuePath;
	}

	private ContainerValueExtractorPath getExtractorPath(Value value) {
		if ( value instanceof org.hibernate.mapping.Collection ) {
			org.hibernate.mapping.Collection collectionValue = (org.hibernate.mapping.Collection) value;
			Class<? extends ContainerValueExtractor> extractorClass = getExtractorClass( collectionValue );
			return ContainerValueExtractorPath.explicitExtractor( extractorClass );
		}
		else {
			return ContainerValueExtractorPath.noExtractors();
		}
	}

	private Class<? extends ContainerValueExtractor> getExtractorClass(org.hibernate.mapping.Collection collectionValue) {
		if ( collectionValue instanceof org.hibernate.mapping.Array ) {
			// Caution if you add other if ( ... instanceof ) branches: Array extends List!
			return ArrayElementExtractor.class;
		}
		else if ( collectionValue instanceof org.hibernate.mapping.Map ) {
			// See contributeModelPropertyNode(), we only care about map values, not about keys
			return MapValueExtractor.class;
		}
		else {
			return CollectionElementExtractor.class;
		}
	}
}
