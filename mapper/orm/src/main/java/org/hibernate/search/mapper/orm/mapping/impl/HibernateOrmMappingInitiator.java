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
import org.hibernate.mapping.PersistentClass;
import org.hibernate.search.engine.cfg.spi.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.cfg.spi.OptionalConfigurationProperty;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.mapper.mapping.spi.MappingBuildContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingConfigurationCollector;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.cfg.impl.HibernateOrmConfigurationPropertySource;
import org.hibernate.search.mapper.orm.mapping.HibernateOrmMappingConfigurationContext;
import org.hibernate.search.mapper.orm.mapping.HibernateOrmSearchMappingConfigurer;
import org.hibernate.search.mapper.orm.model.impl.HibernateOrmBootstrapIntrospector;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoMapperDelegate;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoTypeMetadataContributor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AnnotationMappingConfigurationContext;
import org.hibernate.search.mapper.pojo.mapping.spi.AbstractPojoMappingInitiator;
import org.hibernate.search.util.common.impl.StreamHelper;

public class HibernateOrmMappingInitiator extends AbstractPojoMappingInitiator<HibernateOrmMappingPartialBuildState>
		implements HibernateOrmMappingConfigurationContext {

	private static final ConfigurationProperty<Boolean> ENABLE_ANNOTATION_MAPPING =
			ConfigurationProperty.forKey( HibernateOrmMapperSettings.Radicals.ENABLE_ANNOTATION_MAPPING )
					.asBoolean()
					.withDefault( HibernateOrmMapperSettings.Defaults.ENABLE_ANNOTATION_MAPPING )
					.build();

	private static final OptionalConfigurationProperty<BeanReference<? extends HibernateOrmSearchMappingConfigurer>> MAPPING_CONFIGURER =
			ConfigurationProperty.forKey( HibernateOrmMapperSettings.Radicals.MAPPING_CONFIGURER )
					.asBeanReference( HibernateOrmSearchMappingConfigurer.class )
					.build();

	public static HibernateOrmMappingInitiator create(Metadata metadata, ReflectionManager reflectionManager,
			HibernateOrmConfigurationPropertySource propertySource) {
		HibernateOrmBootstrapIntrospector introspector =
				HibernateOrmBootstrapIntrospector.create( metadata, reflectionManager, propertySource );

		return new HibernateOrmMappingInitiator(
				metadata, propertySource,
				introspector
		);
	}

	private final Metadata metadata;
	private final ConfigurationPropertySource propertySource;
	private final HibernateOrmBootstrapIntrospector introspector;

	private HibernateOrmMappingInitiator(Metadata metadata,
			HibernateOrmConfigurationPropertySource propertySource,
			HibernateOrmBootstrapIntrospector introspector) {
		super( introspector );

		this.metadata = metadata;
		this.propertySource = propertySource;
		this.introspector = introspector;

		/*
		 * This method is called when the session factory is created, and once again when HSearch boots.
		 * It logs a warning when the configuration property is invalid,
		 * so the warning will be logged twice.
		 * Since it only happens when the configuration is invalid,
		 * we can live with this quirk.
		 */
		MultiTenancyStrategy multiTenancyStrategy = MultiTenancyStrategy.determineMultiTenancyStrategy( propertySource.getAllRawProperties() );

		setMultiTenancyEnabled(
				!MultiTenancyStrategy.NONE.equals( multiTenancyStrategy )
		);
	}

	@Override
	public void configure(MappingBuildContext buildContext,
			MappingConfigurationCollector<PojoTypeMetadataContributor> configurationCollector) {
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
		boolean enableAnnotationMapping = ENABLE_ANNOTATION_MAPPING.get( propertySource );
		if ( enableAnnotationMapping ) {
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
