/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.mapping.impl;

import java.util.List;

import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.cfg.spi.OptionalConfigurationProperty;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingBuildContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingConfigurationCollector;
import org.hibernate.search.engine.tenancy.spi.TenancyMode;
import org.hibernate.search.mapper.orm.bootstrap.impl.HibernateSearchPreIntegrationService;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.common.impl.HibernateOrmUtils;
import org.hibernate.search.mapper.orm.coordination.common.spi.CoordinationStrategy;
import org.hibernate.search.mapper.orm.mapping.HibernateOrmMappingConfigurationContext;
import org.hibernate.search.mapper.orm.mapping.HibernateOrmSearchMappingConfigurer;
import org.hibernate.search.mapper.orm.model.impl.HibernateOrmBasicTypeMetadataProvider;
import org.hibernate.search.mapper.orm.model.impl.HibernateOrmBootstrapIntrospector;
import org.hibernate.search.mapper.orm.coordination.impl.CoordinationConfigurationContextImpl;
import org.hibernate.search.mapper.orm.session.impl.ConfiguredAutomaticIndexingStrategy;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoMapperDelegate;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoTypeMetadataContributor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AnnotationMappingConfigurationContext;
import org.hibernate.search.mapper.pojo.mapping.spi.AbstractPojoMappingInitiator;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.reflect.spi.ValueHandleFactory;
import org.hibernate.service.ServiceRegistry;

import org.jboss.jandex.IndexView;

public class HibernateOrmMappingInitiator extends AbstractPojoMappingInitiator<HibernateOrmMappingPartialBuildState>
		implements HibernateOrmMappingConfigurationContext {

	private static final ConfigurationProperty<Boolean> MAPPING_PROCESS_ANNOTATIONS =
			ConfigurationProperty.forKey( HibernateOrmMapperSettings.Radicals.MAPPING_PROCESS_ANNOTATIONS )
					.asBoolean()
					.withDefault( HibernateOrmMapperSettings.Defaults.MAPPING_PROCESS_ANNOTATIONS )
					.build();

	private static final ConfigurationProperty<Boolean> MAPPING_BUILD_MISSING_DISCOVERED_JANDEX_INDEXES =
			ConfigurationProperty.forKey( HibernateOrmMapperSettings.Radicals.MAPPING_BUILD_MISSING_DISCOVERED_JANDEX_INDEXES )
					.asBoolean()
					.withDefault( HibernateOrmMapperSettings.Defaults.MAPPING_BUILD_MISSING_DISCOVERED_JANDEX_INDEXES )
					.build();

	private static final OptionalConfigurationProperty<List<BeanReference<? extends HibernateOrmSearchMappingConfigurer>>> MAPPING_CONFIGURER =
			ConfigurationProperty.forKey( HibernateOrmMapperSettings.Radicals.MAPPING_CONFIGURER )
					.asBeanReference( HibernateOrmSearchMappingConfigurer.class )
					.multivalued()
					.build();

	public static HibernateOrmMappingInitiator create(Metadata metadata, IndexView jandexIndex,
			ReflectionManager reflectionManager,
			ValueHandleFactory valueHandleFactory, ServiceRegistry serviceRegistry) {
		HibernateOrmBasicTypeMetadataProvider basicTypeMetadataProvider =
				HibernateOrmBasicTypeMetadataProvider.create( metadata );
		HibernateOrmBootstrapIntrospector introspector = HibernateOrmBootstrapIntrospector.create(
				basicTypeMetadataProvider, reflectionManager, valueHandleFactory );
		HibernateSearchPreIntegrationService preIntegrationService =
				HibernateOrmUtils.getServiceOrFail( serviceRegistry, HibernateSearchPreIntegrationService.class );

		boolean multiTenancyEnabled = ( (MetadataImplementor) metadata ).getMetadataBuildingOptions()
				.isMultiTenancyEnabled();

		return new HibernateOrmMappingInitiator( basicTypeMetadataProvider, jandexIndex, introspector,
				preIntegrationService, multiTenancyEnabled );
	}

	private final HibernateOrmBasicTypeMetadataProvider basicTypeMetadataProvider;
	private final HibernateOrmBootstrapIntrospector introspector;
	private final HibernateSearchPreIntegrationService preIntegrationService;

	private BeanHolder<? extends CoordinationStrategy> coordinationStrategyHolder;
	private ConfiguredAutomaticIndexingStrategy configuredAutomaticIndexingStrategy;

	private HibernateOrmMappingInitiator(
			HibernateOrmBasicTypeMetadataProvider basicTypeMetadataProvider,
			IndexView jandexIndex, HibernateOrmBootstrapIntrospector introspector,
			HibernateSearchPreIntegrationService preIntegrationService, boolean multiTenancyEnabled) {
		super( introspector );

		this.basicTypeMetadataProvider = basicTypeMetadataProvider;
		if ( jandexIndex != null ) {
			annotationMapping().add( jandexIndex );
		}
		this.introspector = introspector;

		tenancyMode( multiTenancyEnabled ? TenancyMode.MULTI_TENANCY : TenancyMode.SINGLE_TENANCY );

		this.preIntegrationService = preIntegrationService;
	}

	public void closeOnFailure() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.push( ConfiguredAutomaticIndexingStrategy::stop, configuredAutomaticIndexingStrategy );
			closer.push( CoordinationStrategy::stop, coordinationStrategyHolder, BeanHolder::get );
			closer.push( BeanHolder::close, coordinationStrategyHolder );
		}
	}

	@Override
	public void configure(MappingBuildContext buildContext,
			MappingConfigurationCollector<PojoTypeMetadataContributor> configurationCollector) {
		BeanResolver beanResolver = buildContext.beanResolver();
		ConfigurationPropertySource propertySource = buildContext.configurationPropertySource();

		addConfigurationContributor(
				new HibernateOrmMappingConfigurationContributor( basicTypeMetadataProvider, introspector )
		);

		CoordinationConfigurationContextImpl coordinationStrategyConfiguration =
				preIntegrationService.coordinationStrategyConfiguration();
		coordinationStrategyHolder = coordinationStrategyConfiguration.strategyHolder();
		configuredAutomaticIndexingStrategy = coordinationStrategyConfiguration.createAutomaticIndexingStrategy();

		// If the automatic indexing strategy uses an event queue,
		// it will need to send events relative to contained entities,
		// and thus contained entities need to have an identity mapping.
		containedEntityIdentityMappingRequired( configuredAutomaticIndexingStrategy.usesAsyncProcessing() );

		// Enable annotation mapping if necessary
		boolean processAnnotations = MAPPING_PROCESS_ANNOTATIONS.get( propertySource );
		if ( processAnnotations ) {
			annotationMapping()
					.discoverAnnotatedTypesFromRootMappingAnnotations( true )
					.discoverJandexIndexesFromAddedTypes( true )
					.buildMissingDiscoveredJandexIndexes( MAPPING_BUILD_MISSING_DISCOVERED_JANDEX_INDEXES.get( propertySource ) )
					.discoverAnnotationsFromReferencedTypes( true );

			AnnotationMappingConfigurationContext annotationMapping = annotationMapping();
			for ( PersistentClass persistentClass : basicTypeMetadataProvider.getPersistentClasses() ) {
				if ( persistentClass.hasPojoRepresentation() ) {
					annotationMapping.add( persistentClass.getMappedClass() );
				}
			}
		}

		// Apply the user-provided mapping configurer if necessary
		MAPPING_CONFIGURER.getAndMap( propertySource, beanResolver::resolve )
				.ifPresent( holder -> {
					try ( BeanHolder<List<HibernateOrmSearchMappingConfigurer>> configurerHolder = holder ) {
						for ( HibernateOrmSearchMappingConfigurer configurer : configurerHolder.get() ) {
							configurer.configure( this );
						}
					}
				} );

		super.configure( buildContext, configurationCollector );
	}

	@Override
	protected PojoMapperDelegate<HibernateOrmMappingPartialBuildState> createMapperDelegate() {
		return new HibernateOrmMapperDelegate( basicTypeMetadataProvider, coordinationStrategyHolder,
				configuredAutomaticIndexingStrategy );
	}
}
