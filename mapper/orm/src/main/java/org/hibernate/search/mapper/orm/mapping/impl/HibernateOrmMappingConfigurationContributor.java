/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.mapping.impl;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Value;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingBuildContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingConfigurationCollector;
import org.hibernate.search.mapper.orm.common.impl.HibernateOrmUtils;
import org.hibernate.search.mapper.orm.model.impl.HibernateOrmBasicTypeMetadataProvider;
import org.hibernate.search.mapper.orm.model.impl.HibernateOrmBootstrapIntrospector;
import org.hibernate.search.mapper.pojo.mapping.building.spi.ErrorCollectingPojoTypeMetadataContributor;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoTypeMetadataContributor;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingConfigurationContext;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingConfigurationContributor;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;

/**
 * Translates metadata from the Hibernate ORM mapping into metadata for the Hibernate Search mapping,
 * and contributes that metadata to the Hibernate Search mapping configuration.
 * <p>
 * This includes in particular metadata about the inverse side of associations,
 * but is not limited to that (there's some metadata about BigDecimal scale, for example).
 */
public final class HibernateOrmMappingConfigurationContributor implements PojoMappingConfigurationContributor {
	private final HibernateOrmBasicTypeMetadataProvider basicTypeMetadataProvider;
	private final HibernateOrmBootstrapIntrospector introspector;

	HibernateOrmMappingConfigurationContributor(HibernateOrmBasicTypeMetadataProvider basicTypeMetadataProvider,
			HibernateOrmBootstrapIntrospector introspector) {
		this.basicTypeMetadataProvider = basicTypeMetadataProvider;
		this.introspector = introspector;
	}

	@Override
	public void configure(MappingBuildContext buildContext, PojoMappingConfigurationContext configurationContext,
			MappingConfigurationCollector<PojoTypeMetadataContributor> configurationCollector) {
		Set<PojoRawTypeModel<?>> processedEmbeddableTypes = new LinkedHashSet<>();

		for ( PersistentClass persistentClass : basicTypeMetadataProvider.getPersistentClasses() ) {
			Class<?> clazz = persistentClass.getMappedClass();
			PojoRawTypeModel<?> typeModel;
			if ( persistentClass.hasPojoRepresentation() ) {
				typeModel = introspector.typeModel( clazz );
			}
			else {
				typeModel = introspector.typeModel( persistentClass.getEntityName() );
			}

			// Sort the properties before processing for deterministic iteration
			List<Property> properties =
					HibernateOrmUtils.sortedNonSyntheticProperties( persistentClass.getProperties().iterator() );

			Property identifierProperty = persistentClass.getIdentifierProperty();
			Optional<Property> identifierPropertyOptional = Optional.ofNullable( identifierProperty );

			// include ID in the properties list for additional metadata contribution.
			// as the list of properties is supposed to be sorted, we put id as the first element.
			identifierPropertyOptional.ifPresent( identifier -> properties.add( 0, identifier ) );

			configurationCollector.collectContributor(
					typeModel,
					new ErrorCollectingPojoTypeMetadataContributor()
							// Ensure entities are declared as such
							.add( new HibernateOrmEntityTypeMetadataContributor(
									typeModel, persistentClass, identifierPropertyOptional.map( Property::getName )
							) )
							// Ensure Hibernate ORM metadata about properties is translated into Hibernate Search metadata
							.add( new HibernateOrmMappingPropertiesMetadataContributor(
									basicTypeMetadataProvider, properties
							) )
			);

			// Also collect metadata about all embeddable types referenced from this entity type.
			contributeEmbeddableTypeMetadata( configurationCollector, processedEmbeddableTypes, properties );
		}
	}

	@SuppressWarnings("rawtypes")
	private void contributeEmbeddableTypeMetadata(
			MappingConfigurationCollector<PojoTypeMetadataContributor> configurationCollector,
			Set<PojoRawTypeModel<?>> processedEmbeddableTypes,
			List<Property> properties) {
		for ( Property property : properties ) {
			contributeEmbeddableTypeMetadata( configurationCollector, processedEmbeddableTypes, property );
		}
	}

	private void contributeEmbeddableTypeMetadata(
			MappingConfigurationCollector<PojoTypeMetadataContributor> configurationCollector,
			Set<PojoRawTypeModel<?>> processedEmbeddableTypes,
			Property property) {
		Value value = property.getValue();
		if ( value instanceof Component ) {
			Component componentValue = (Component) value;
			PojoRawTypeModel<?> componentTypeModel;
			if ( componentValue.isDynamic() ) {
				componentTypeModel = introspector.typeModel( componentValue.getRoleName() );
			}
			else {
				componentTypeModel = introspector.typeModel( (Class<?>) componentValue.getComponentClass() );
			}
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
			if ( processedEmbeddableTypes.add( componentTypeModel ) ) {
				// Sort the properties before processing for deterministic iteration
				List<Property> properties =
						HibernateOrmUtils.sortedNonSyntheticProperties( componentValue.getProperties().iterator() );
				configurationCollector.collectContributor( componentTypeModel,
						new ErrorCollectingPojoTypeMetadataContributor()
								// Ensure Hibernate ORM metadata about properties is translated into Hibernate Search metadata
								.add( new HibernateOrmMappingPropertiesMetadataContributor(
										basicTypeMetadataProvider, properties
								) ) );
				// Recurse in order to find embeddeds within embeddeds
				contributeEmbeddableTypeMetadata( configurationCollector, processedEmbeddableTypes, properties );
			}
		}
	}

}
