/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.standalone.mapping.impl;

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
import org.hibernate.search.mapper.pojo.standalone.model.impl.StandalonePojoBootstrapIntrospector;

public class StandalonePojoMappingInitiator extends AbstractPojoMappingInitiator<StandalonePojoMappingPartialBuildState>
		implements StandalonePojoMappingConfigurationContext {

	private static final OptionalConfigurationProperty<
			List<BeanReference<? extends StandalonePojoMappingConfigurer>>> MAPPING_CONFIGURER =
					ConfigurationProperty.forKey( StandalonePojoMapperSettings.Radicals.MAPPING_CONFIGURER )
							.asBeanReference( StandalonePojoMappingConfigurer.class )
							.multivalued()
							.build();

	private static final ConfigurationProperty<Boolean> MULTI_TENANCY_ENABLED =
			ConfigurationProperty.forKey( StandalonePojoMapperSettings.Radicals.MULTI_TENANCY_ENABLED )
					.asBoolean()
					.withDefault( StandalonePojoMapperSettings.Defaults.MULTI_TENANCY_ENABLED )
					.build();

	public StandalonePojoMappingInitiator(StandalonePojoBootstrapIntrospector introspector) {
		super( introspector );
		// Enable annotated type discovery by default
		annotationMapping()
				.discoverAnnotatedTypesFromRootMappingAnnotations( true )
				.discoverJandexIndexesFromAddedTypes( true )
				.discoverAnnotationsFromReferencedTypes( true );
	}

	@Override
	public void configure(MappingBuildContext buildContext,
			MappingConfigurationCollector<PojoTypeMetadataContributor> configurationCollector) {
		this.tenancyMode(
				MULTI_TENANCY_ENABLED.get( buildContext.configurationPropertySource() )
						? TenancyMode.MULTI_TENANCY
						: TenancyMode.SINGLE_TENANCY
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

		super.configure( buildContext, configurationCollector );
	}

	@Override
	protected PojoMapperDelegate<StandalonePojoMappingPartialBuildState> createMapperDelegate() {
		return new StandalonePojoMapperDelegate();
	}
}
