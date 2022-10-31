/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.standalone.impl;

import java.util.List;

import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.cfg.spi.OptionalConfigurationProperty;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingBuildContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingConfigurationCollector;
import org.hibernate.search.engine.tenancy.spi.TenancyMode;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoMapperDelegate;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoTypeMetadataContributor;
import org.hibernate.search.mapper.pojo.mapping.spi.AbstractPojoMappingInitiator;
import org.hibernate.search.mapper.pojo.standalone.cfg.StandalonePojoMapperSettings;
import org.hibernate.search.mapper.pojo.standalone.mapping.StandalonePojoMappingConfigurationContext;
import org.hibernate.search.mapper.pojo.standalone.mapping.StandalonePojoMappingConfigurer;
import org.hibernate.search.mapper.pojo.standalone.mapping.impl.StandalonePojoMapperDelegate;
import org.hibernate.search.mapper.pojo.standalone.mapping.impl.StandalonePojoMappingPartialBuildState;
import org.hibernate.search.mapper.pojo.standalone.mapping.metadata.EntityConfigurer;
import org.hibernate.search.mapper.pojo.standalone.mapping.metadata.impl.StandalonePojoEntityTypeMetadataProvider;
import org.hibernate.search.mapper.pojo.standalone.model.impl.StandalonePojoBootstrapIntrospector;
import org.hibernate.search.mapper.pojo.standalone.schema.management.SchemaManagementStrategyName;
import org.hibernate.search.mapper.pojo.standalone.schema.management.impl.SchemaManagementListener;

public class StandalonePojoMappingInitiator extends AbstractPojoMappingInitiator<StandalonePojoMappingPartialBuildState>
		implements StandalonePojoMappingConfigurationContext {

	private static final ConfigurationProperty<SchemaManagementStrategyName> SCHEMA_MANAGEMENT_STRATEGY
			= ConfigurationProperty.forKey( StandalonePojoMapperSettings.Radicals.SCHEMA_MANAGEMENT_STRATEGY )
			.as( SchemaManagementStrategyName.class, SchemaManagementStrategyName::of )
			.withDefault( StandalonePojoMapperSettings.Defaults.SCHEMA_MANAGEMENT_STRATEGY )
			.build();

	private static final OptionalConfigurationProperty<List<BeanReference<? extends StandalonePojoMappingConfigurer>>> MAPPING_CONFIGURER =
			ConfigurationProperty.forKey( StandalonePojoMapperSettings.Radicals.MAPPING_CONFIGURER )
					.asBeanReference( StandalonePojoMappingConfigurer.class )
					.multivalued()
					.build();

	private static final ConfigurationProperty<Boolean> MULTI_TENANCY_ENABLED =
			ConfigurationProperty.forKey( StandalonePojoMapperSettings.Radicals.MULTI_TENANCY_ENABLED )
					.asBoolean()
					.withDefault( StandalonePojoMapperSettings.Defaults.MULTI_TENANCY_ENABLED )
					.build();

	private final StandalonePojoEntityTypeMetadataProvider.Builder entityTypeMetadataProviderBuilder;

	private StandalonePojoEntityTypeMetadataProvider entityTypeMetadataProvider;
	private SchemaManagementListener schemaManagementListener;

	public StandalonePojoMappingInitiator(StandalonePojoBootstrapIntrospector introspector) {
		super( introspector );
		entityTypeMetadataProviderBuilder = new StandalonePojoEntityTypeMetadataProvider.Builder( introspector );
	}

	public <E> StandalonePojoMappingInitiator addEntityType(Class<E> clazz, String entityName,
			EntityConfigurer<E> configurerOrNull) {
		entityTypeMetadataProviderBuilder.addEntityType( clazz, entityName, configurerOrNull );

		return this;
	}

	@Override
	public void configure(MappingBuildContext buildContext,
			MappingConfigurationCollector<PojoTypeMetadataContributor> configurationCollector) {
		this.tenancyMode(
				MULTI_TENANCY_ENABLED.get( buildContext.configurationPropertySource() ) ?
						TenancyMode.MULTI_TENANCY : TenancyMode.SINGLE_TENANCY
		);
		// Apply the user-provided mapping configurer if necessary.
		// Has to happen before building entityTypeMetadataProvider as configurers can add more entities.
		MAPPING_CONFIGURER.getAndMap( buildContext.configurationPropertySource(), buildContext.beanResolver()::resolve )
				.ifPresent( holder -> {
					try ( BeanHolder<List<StandalonePojoMappingConfigurer>> configurerHolder = holder ) {
						for ( StandalonePojoMappingConfigurer configurer : configurerHolder.get() ) {
							configurer.configure( this );
						}
					}
				} );

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
