/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.standalone.mapping.impl;

import java.util.List;

import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
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
import org.hibernate.search.mapper.pojo.standalone.reporting.impl.StandalonePojoMapperHints;

public class StandalonePojoMappingInitiator extends AbstractPojoMappingInitiator<StandalonePojoMappingPartialBuildState>
		implements StandalonePojoMappingConfigurationContext {

	private static final ConfigurationProperty<Boolean> MAPPING_BUILD_MISSING_DISCOVERED_JANDEX_INDEXES =
			ConfigurationProperty
					.forKey( StandalonePojoMapperSettings.Radicals.MAPPING_BUILD_MISSING_DISCOVERED_JANDEX_INDEXES )
					.asBoolean()
					.withDefault( StandalonePojoMapperSettings.Defaults.MAPPING_BUILD_MISSING_DISCOVERED_JANDEX_INDEXES )
					.build();

	private static final ConfigurationProperty<Boolean> MAPPING_DISCOVER_ANNOTATED_TYPES_FROM_ROOT_MAPPING_ANNOTATIONS =
			ConfigurationProperty.forKey(
					StandalonePojoMapperSettings.Radicals.MAPPING_DISCOVER_ANNOTATED_TYPES_FROM_ROOT_MAPPING_ANNOTATIONS )
					.asBoolean()
					.withDefault(
							StandalonePojoMapperSettings.Defaults.MAPPING_DISCOVER_ANNOTATED_TYPES_FROM_ROOT_MAPPING_ANNOTATIONS )
					.build();

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
		super( introspector, StandalonePojoMapperHints.INSTANCE );
	}

	@Override
	public void configure(MappingBuildContext buildContext,
			MappingConfigurationCollector<PojoTypeMetadataContributor> configurationCollector) {
		ConfigurationPropertySource propertySource = buildContext.configurationPropertySource();
		this.tenancyMode(
				MULTI_TENANCY_ENABLED.get( propertySource )
						? TenancyMode.MULTI_TENANCY
						: TenancyMode.SINGLE_TENANCY
		);

		// Enable annotated type discovery by default
		annotationMapping()
				.discoverAnnotatedTypesFromRootMappingAnnotations(
						MAPPING_DISCOVER_ANNOTATED_TYPES_FROM_ROOT_MAPPING_ANNOTATIONS.get( propertySource ) )
				.discoverJandexIndexesFromAddedTypes( true )
				.buildMissingDiscoveredJandexIndexes(
						MAPPING_BUILD_MISSING_DISCOVERED_JANDEX_INDEXES.get( propertySource ) )
				.discoverAnnotationsFromReferencedTypes( true );

		// Apply the user-provided mapping configurer if necessary.
		// Has to happen before building entityTypeMetadataProvider as configurers can add more entities.
		MAPPING_CONFIGURER.getAndMap( propertySource, buildContext.beanResolver()::resolve )
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
