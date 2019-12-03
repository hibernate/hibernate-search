/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.mapping.impl;

import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

import org.hibernate.MultiTenancyStrategy;
import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.boot.Metadata;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.search.engine.cfg.spi.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.cfg.spi.OptionalConfigurationProperty;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingBuildContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingConfigurationCollector;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.mapping.HibernateOrmMappingConfigurationContext;
import org.hibernate.search.mapper.orm.mapping.HibernateOrmSearchMappingConfigurer;
import org.hibernate.search.mapper.orm.model.impl.HibernateOrmBasicTypeMetadataProvider;
import org.hibernate.search.mapper.orm.model.impl.HibernateOrmBootstrapIntrospector;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoMapperDelegate;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoTypeMetadataContributor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AnnotationMappingConfigurationContext;
import org.hibernate.search.mapper.pojo.mapping.spi.AbstractPojoMappingInitiator;
import org.hibernate.search.util.common.impl.StreamHelper;

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

	public static HibernateOrmMappingInitiator create(Metadata metadata, ReflectionManager reflectionManager,
			ConfigurationService ormConfigurationService,
			ConfigurationPropertySource propertySource) {
		HibernateOrmBasicTypeMetadataProvider basicTypeMetadataProvider =
				HibernateOrmBasicTypeMetadataProvider.create( metadata );
		HibernateOrmBootstrapIntrospector introspector =
				HibernateOrmBootstrapIntrospector.create( basicTypeMetadataProvider, reflectionManager, propertySource );

		return new HibernateOrmMappingInitiator(
				metadata, ormConfigurationService, introspector
		);
	}

	private final Metadata metadata;
	private final HibernateOrmBootstrapIntrospector introspector;

	private HibernateOrmMappingInitiator(Metadata metadata,
			ConfigurationService ormConfigurationService,
			HibernateOrmBootstrapIntrospector introspector) {
		super( introspector );

		this.metadata = metadata;
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

		setMultiTenancyEnabled(
				!MultiTenancyStrategy.NONE.equals( multiTenancyStrategy )
		);
	}

	@Override
	public void configure(MappingBuildContext buildContext,
			MappingConfigurationCollector<PojoTypeMetadataContributor> configurationCollector) {
		ConfigurationPropertySource propertySource = buildContext.getConfigurationPropertySource();

		Map<String, PersistentClass> persistentClasses = metadata.getEntityBindings().stream()
				// getMappedClass() can return null, which should be ignored
				.filter( persistentClass -> persistentClass.getMappedClass() != null )
				.collect( StreamHelper.toMap(
						PersistentClass::getEntityName,
						Function.identity(),
						/*
						 * The entity bindings are stored in a HashMap whose order is not well defined.
						 * Copy them to a sorted map before processing for deterministic iteration.
						 */
						TreeMap::new
				) );

		addConfigurationContributor(
				new HibernateOrmMetatadaContributor( introspector, persistentClasses )
		);

		// Enable annotation mapping if necessary
		boolean processAnnotations = MAPPING_PROCESS_ANNOTATIONS.get( propertySource );
		if ( processAnnotations ) {
			setAnnotatedTypeDiscoveryEnabled( true );

			AnnotationMappingConfigurationContext annotationMapping = annotationMapping();
			for ( PersistentClass persistentClass : persistentClasses.values() ) {
				annotationMapping.add( persistentClass.getMappedClass() );
			}
		}

		// Apply the user-provided mapping configurer if necessary
		final BeanResolver beanResolver = buildContext.getBeanResolver();
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
		return new HibernateOrmMapperDelegate();
	}
}
