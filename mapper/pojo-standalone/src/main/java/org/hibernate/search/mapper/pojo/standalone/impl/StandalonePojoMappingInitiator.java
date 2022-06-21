/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.standalone.impl;

import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingBuildContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingConfigurationCollector;
import org.hibernate.search.mapper.pojo.standalone.cfg.StandalonePojoMapperSettings;
import org.hibernate.search.mapper.pojo.standalone.mapping.impl.StandalonePojoMapperDelegate;
import org.hibernate.search.mapper.pojo.standalone.mapping.impl.StandalonePojoMappingPartialBuildState;
import org.hibernate.search.mapper.pojo.standalone.mapping.metadata.EntityConfigurer;
import org.hibernate.search.mapper.pojo.standalone.mapping.metadata.impl.StandalonePojoEntityTypeMetadataProvider;
import org.hibernate.search.mapper.pojo.standalone.model.impl.StandalonePojoBootstrapIntrospector;
import org.hibernate.search.mapper.pojo.standalone.schema.management.SchemaManagementStrategyName;
import org.hibernate.search.mapper.pojo.standalone.schema.management.impl.SchemaManagementListener;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoMapperDelegate;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoTypeMetadataContributor;
import org.hibernate.search.mapper.pojo.mapping.spi.AbstractPojoMappingInitiator;

public class StandalonePojoMappingInitiator extends AbstractPojoMappingInitiator<StandalonePojoMappingPartialBuildState> {

	private static final ConfigurationProperty<SchemaManagementStrategyName> SCHEMA_MANAGEMENT_STRATEGY
			= ConfigurationProperty.forKey( StandalonePojoMapperSettings.Radicals.SCHEMA_MANAGEMENT_STRATEGY )
					.as( SchemaManagementStrategyName.class, SchemaManagementStrategyName::of )
					.withDefault( StandalonePojoMapperSettings.Defaults.SCHEMA_MANAGEMENT_STRATEGY )
					.build();

	private final StandalonePojoEntityTypeMetadataProvider.Builder entityTypeMetadataProviderBuilder;

	private StandalonePojoEntityTypeMetadataProvider entityTypeMetadataProvider;
	private SchemaManagementListener schemaManagementListener;

	public StandalonePojoMappingInitiator(StandalonePojoBootstrapIntrospector introspector) {
		super( introspector );
		entityTypeMetadataProviderBuilder = new StandalonePojoEntityTypeMetadataProvider.Builder( introspector );
	}

	public <E> void addEntityType(Class<E> clazz, String entityName, EntityConfigurer<E> configurerOrNull) {
		entityTypeMetadataProviderBuilder.addEntityType( clazz, entityName, configurerOrNull );
	}

	@Override
	public void configure(MappingBuildContext buildContext,
			MappingConfigurationCollector<PojoTypeMetadataContributor> configurationCollector) {
		entityTypeMetadataProvider = entityTypeMetadataProviderBuilder.build();

		SchemaManagementStrategyName schemaManagementStrategyName = SCHEMA_MANAGEMENT_STRATEGY.get(
				buildContext.configurationPropertySource() );
		schemaManagementListener = new SchemaManagementListener( schemaManagementStrategyName );

		addConfigurationContributor( new StandalonePojoTypeConfigurationContributor( entityTypeMetadataProvider ) );
		super.configure( buildContext, configurationCollector );
	}

	@Override
	protected PojoMapperDelegate<StandalonePojoMappingPartialBuildState> createMapperDelegate() {
		return new StandalonePojoMapperDelegate( entityTypeMetadataProvider, schemaManagementListener );
	}
}
