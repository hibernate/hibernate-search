/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.mapping.impl;

import org.hibernate.MultiTenancyStrategy;
import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.boot.Metadata;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.cfg.spi.OptionalConfigurationProperty;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingBuildContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingConfigurationCollector;
import org.hibernate.search.mapper.orm.automaticindexing.AutomaticIndexingStrategyNames;
import org.hibernate.search.mapper.orm.automaticindexing.spi.AutomaticIndexingStrategy;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.mapping.HibernateOrmMappingConfigurationContext;
import org.hibernate.search.mapper.orm.mapping.HibernateOrmSearchMappingConfigurer;
import org.hibernate.search.mapper.orm.model.impl.HibernateOrmBasicTypeMetadataProvider;
import org.hibernate.search.mapper.orm.model.impl.HibernateOrmBootstrapIntrospector;
import org.hibernate.search.mapper.orm.session.impl.ConfiguredAutomaticIndexingStrategy;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoMapperDelegate;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoTypeMetadataContributor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AnnotationMappingConfigurationContext;
import org.hibernate.search.mapper.pojo.mapping.spi.AbstractPojoMappingInitiator;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.reflect.spi.ValueReadHandleFactory;

public class HibernateOrmMappingInitiator extends AbstractPojoMappingInitiator<HibernateOrmMappingPartialBuildState>
		implements HibernateOrmMappingConfigurationContext {

	private static final ConfigurationProperty<Boolean> MAPPING_PROCESS_ANNOTATIONS =
			ConfigurationProperty.forKey( HibernateOrmMapperSettings.Radicals.MAPPING_PROCESS_ANNOTATIONS )
					.asBoolean()
					.withDefault( HibernateOrmMapperSettings.Defaults.MAPPING_PROCESS_ANNOTATIONS )
					.build();

	private static final OptionalConfigurationProperty<BeanReference<? extends HibernateOrmSearchMappingConfigurer>> MAPPING_CONFIGURER =
			ConfigurationProperty.forKey( HibernateOrmMapperSettings.Radicals.MAPPING_CONFIGURER )
					.asBeanReference( HibernateOrmSearchMappingConfigurer.class )
					.build();

	@SuppressWarnings("deprecation")
	private static final ConfigurationProperty<BeanReference<? extends AutomaticIndexingStrategy>> AUTOMATIC_INDEXING_STRATEGY =
			ConfigurationProperty.forKey( HibernateOrmMapperSettings.Radicals.AUTOMATIC_INDEXING_STRATEGY )
					.asBeanReference( AutomaticIndexingStrategy.class )
					.substitute( org.hibernate.search.mapper.orm.automaticindexing.AutomaticIndexingStrategyName.NONE, AutomaticIndexingStrategyNames.NONE )
					.substitute( org.hibernate.search.mapper.orm.automaticindexing.AutomaticIndexingStrategyName.SESSION, AutomaticIndexingStrategyNames.SESSION )
					.withDefault( HibernateOrmMapperSettings.Defaults.AUTOMATIC_INDEXING_STRATEGY )
					.build();

	public static HibernateOrmMappingInitiator create(Metadata metadata, ReflectionManager reflectionManager,
			ValueReadHandleFactory valueReadHandleFactory, ConfigurationService ormConfigurationService) {
		HibernateOrmBasicTypeMetadataProvider basicTypeMetadataProvider =
				HibernateOrmBasicTypeMetadataProvider.create( metadata );
		HibernateOrmBootstrapIntrospector introspector = HibernateOrmBootstrapIntrospector.create(
				basicTypeMetadataProvider, reflectionManager, valueReadHandleFactory );

		return new HibernateOrmMappingInitiator( basicTypeMetadataProvider, introspector, ormConfigurationService );
	}

	private final HibernateOrmBasicTypeMetadataProvider basicTypeMetadataProvider;
	private final HibernateOrmBootstrapIntrospector introspector;

	private BeanHolder<? extends AutomaticIndexingStrategy> automaticIndexingStrategyHolder;
	private ConfiguredAutomaticIndexingStrategy configuredAutomaticIndexingStrategy;

	private HibernateOrmMappingInitiator(HibernateOrmBasicTypeMetadataProvider basicTypeMetadataProvider,
			HibernateOrmBootstrapIntrospector introspector, ConfigurationService ormConfigurationService) {
		super( introspector );

		this.basicTypeMetadataProvider = basicTypeMetadataProvider;
		this.introspector = introspector;

		/*
		 * This method is called when the session factory is created, and once again when HSearch boots.
		 * It logs a warning when the configuration property is invalid,
		 * so the warning will be logged twice.
		 * Since it only happens when the configuration is invalid,
		 * we can live with this quirk.
		 */
		MultiTenancyStrategy multiTenancyStrategy =
				MultiTenancyStrategy.determineMultiTenancyStrategy( ormConfigurationService.getSettings() );

		multiTenancyEnabled(
				!MultiTenancyStrategy.NONE.equals( multiTenancyStrategy )
		);
	}

	public void closeOnFailure() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.push( ConfiguredAutomaticIndexingStrategy::stop, configuredAutomaticIndexingStrategy );
			closer.push( AutomaticIndexingStrategy::stop, automaticIndexingStrategyHolder, BeanHolder::get );
			closer.push( BeanHolder::close, automaticIndexingStrategyHolder );
		}
	}

	@Override
	public void configure(MappingBuildContext buildContext,
			MappingConfigurationCollector<PojoTypeMetadataContributor> configurationCollector) {
		BeanResolver beanResolver = buildContext.beanResolver();
		ConfigurationPropertySource propertySource = buildContext.configurationPropertySource();

		addConfigurationContributor(
				new HibernateOrmMetatadaContributor( basicTypeMetadataProvider, introspector )
		);

		ConfiguredAutomaticIndexingStrategy.Builder builder = new ConfiguredAutomaticIndexingStrategy.Builder();
		automaticIndexingStrategyHolder =
				AUTOMATIC_INDEXING_STRATEGY.getAndTransform( propertySource, beanResolver::resolve );
		automaticIndexingStrategyHolder.get().configure( builder );
		configuredAutomaticIndexingStrategy = builder.build();

		// If the automatic indexing strategy uses an event queue,
		// it will need to send events relative to contained entities,
		// and thus contained entities need to have an identity mapping.
		containedEntityIdentityMappingRequired( configuredAutomaticIndexingStrategy.usesEventQueue() );

		// Enable annotation mapping if necessary
		boolean processAnnotations = MAPPING_PROCESS_ANNOTATIONS.get( propertySource );
		if ( processAnnotations ) {
			annotatedTypeDiscoveryEnabled( true );

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
					try ( BeanHolder<? extends HibernateOrmSearchMappingConfigurer> configurerHolder = holder ) {
						configurerHolder.get().configure( this );
					}
				} );

		super.configure( buildContext, configurationCollector );
	}

	@Override
	protected PojoMapperDelegate<HibernateOrmMappingPartialBuildState> createMapperDelegate() {
		return new HibernateOrmMapperDelegate( basicTypeMetadataProvider, automaticIndexingStrategyHolder,
				configuredAutomaticIndexingStrategy );
	}
}
