/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.mapping.impl;

import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import org.hibernate.boot.Metadata;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.OneToMany;
import org.hibernate.mapping.OneToOne;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.ToOne;
import org.hibernate.mapping.Value;
import org.hibernate.search.mapper.orm.model.impl.HibernateOrmBasicTypeMetadataProvider;
import org.hibernate.search.mapper.pojo.extractor.builtin.BuiltinContainerExtractors;
import org.hibernate.search.mapper.pojo.extractor.mapping.programmatic.ContainerExtractorPath;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoTypeMetadataContributor;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.building.spi.PojoAdditionalMetadataCollectorPropertyNode;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.building.spi.PojoAdditionalMetadataCollectorTypeNode;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPath;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPathValueNode;

/**
 * Translates metadata from properties in the Hibernate ORM mapping
 * into Hibernate Search metadata.
 * <p>
 * This includes in particular metadata about the inverse side of associations,
 * but is not limited to that (there's some metadata about BigDecimal scale, for example).
 */
public final class HibernateOrmMappingPropertiesMetadataContributor implements PojoTypeMetadataContributor {
	private final HibernateOrmBasicTypeMetadataProvider basicTypeMetadataProvider;
	private final List<Property> properties;

	HibernateOrmMappingPropertiesMetadataContributor(HibernateOrmBasicTypeMetadataProvider basicTypeMetadataProvider,
			List<Property> properties) {
		this.basicTypeMetadataProvider = basicTypeMetadataProvider;
		this.properties = properties;
	}

	@Override
	public void contributeAdditionalMetadata(PojoAdditionalMetadataCollectorTypeNode collector) {
		for ( Property property : properties ) {
			collector.property( property.getName(), collectorPropertyNode ->
					collectMetadataFromHibernateOrmMappingProperty( collectorPropertyNode, property ) );
		}
	}

	private void collectMetadataFromHibernateOrmMappingProperty(PojoAdditionalMetadataCollectorPropertyNode collector,
			Property property) {
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
				collector.value( getExtractorPath( collectionValue ) )
						.associationInverseSide( resolveMappedByPath( referencedEntityName, mappedByPath ) );
			}
		}
		else if ( value instanceof OneToOne ) {
			// ManyToOne never carries any useful inverse side information:
			// either it refers to nothing or to a synthetic property that we're not interested in.
			// OneToOne, on the other hand, uses ReferencedPropertyName to store the "mappedBy" information.
			OneToOne toOneValue = (OneToOne) value;
			String referencedEntityName = toOneValue.getReferencedEntityName();
			String mappedByPath = toOneValue.getReferencedPropertyName();
			if ( mappedByPath != null && !mappedByPath.isEmpty() ) {
				collector.value( getExtractorPath( toOneValue ) )
						.associationInverseSide( resolveMappedByPath( referencedEntityName, mappedByPath ) );
			}
		}
		else if ( value instanceof Component ) {
			collector.value( getExtractorPath( value ) )
					.associationEmbedded();
		}
		else if ( value instanceof SimpleValue ) {
			collectScale( collector, value );
		}
	}

	private void collectScale(PojoAdditionalMetadataCollectorPropertyNode collector, Value value) {
		Iterator<Selectable> columnIterator = value.getColumnIterator();
		Dialect dialect = basicTypeMetadataProvider.getDialect();
		Metadata metadata = basicTypeMetadataProvider.getMetadata();

		while ( columnIterator.hasNext() ) {
			Selectable mappedColumn = columnIterator.next();
			if ( !(mappedColumn instanceof Column) ) {
				continue;
			}
			Column column = (Column) mappedColumn;
			Size size = column.getColumnSize( dialect, metadata );
			Integer scale = size.getScale();
			if ( scale == null ) {
				continue;
			}
			collector.value( getExtractorPath( value ) ).decimalScale( scale );
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
		Property property = basicTypeMetadataProvider.getPersistentClass( inverseSideEntity ).getProperty( rootPropertyName );

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
			return BuiltinContainerExtractors.ARRAY_OBJECT;
		}
		else if ( collectionValue instanceof org.hibernate.mapping.Map ) {
			// See contributeModelPropertyNode(), we only care about map values, not about keys
			return BuiltinContainerExtractors.MAP_VALUE;
		}
		else {
			return BuiltinContainerExtractors.COLLECTION;
		}
	}

}
