/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.mapping.impl;

import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;

import org.hibernate.MultiTenancyStrategy;
import org.hibernate.boot.Metadata;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.environment.bean.BeanProvider;
import org.hibernate.search.engine.mapper.mapping.spi.MappingBuildContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingConfigurationCollector;
import org.hibernate.search.mapper.orm.cfg.SearchOrmSettings;
import org.hibernate.search.mapper.orm.mapping.spi.HibernateOrmMapping;
import org.hibernate.search.mapper.orm.mapping.HibernateOrmMappingDefinitionContainerContext;
import org.hibernate.search.mapper.orm.mapping.HibernateOrmSearchMappingConfigurer;
import org.hibernate.search.mapper.orm.model.impl.HibernateOrmBootstrapIntrospector;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoTypeMetadataContributor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AnnotationMappingDefinitionContext;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingInitiatorImpl;
import org.hibernate.search.util.impl.common.StreamHelper;

/*
 * TODO make the following additions to the Hibernate ORM specific mapping:
 *  1. When the @DocumentId is the @Id, use the provided ID in priority and only if it's missing, unproxy the entity and get the ID;
 *     when the @DocumentId is NOT the @Id, always ignore the provided ID. See org.hibernate.search.engine.common.impl.WorkPlan.PerClassWork.extractProperId(Work)
 *  2. And more?
 */
public class HibernateOrmMappingInitiator extends PojoMappingInitiatorImpl<HibernateOrmMapping>
		implements HibernateOrmMappingDefinitionContainerContext {

	private static final ConfigurationProperty<Boolean> ENABLE_ANNOTATION_MAPPING =
			ConfigurationProperty.forKey( SearchOrmSettings.Radicals.ENABLE_ANNOTATION_MAPPING )
					.asBoolean()
					.withDefault( SearchOrmSettings.Defaults.ENABLE_ANNOTATION_MAPPING )
					.build();

	public static HibernateOrmMappingInitiator create(Metadata metadata,
			SessionFactoryImplementor sessionFactoryImplementor) {
		HibernateOrmBootstrapIntrospector introspector =
				new HibernateOrmBootstrapIntrospector( metadata, sessionFactoryImplementor );

		return new HibernateOrmMappingInitiator(
				metadata,
				introspector, sessionFactoryImplementor
		);
	}

	private final Metadata metadata;
	private final HibernateOrmBootstrapIntrospector introspector;

	private HibernateOrmMappingInitiator(Metadata metadata,
			HibernateOrmBootstrapIntrospector introspector,
			SessionFactoryImplementor sessionFactoryImplementor) {
		super(
				new HibernateOrmMappingFactory( sessionFactoryImplementor ),
				introspector
		);

		this.metadata = metadata;
		this.introspector = introspector;

		setMultiTenancyEnabled(
				!MultiTenancyStrategy.NONE.equals( sessionFactoryImplementor.getSessionFactoryOptions().getMultiTenancyStrategy() )
		);
	}

	@Override
	public void configure(MappingBuildContext buildContext, ConfigurationPropertySource propertySource,
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

			AnnotationMappingDefinitionContext annotationMapping = annotationMapping();
			for ( PersistentClass persistentClass : persistentClasses.values() ) {
				annotationMapping.add( persistentClass.getMappedClass() );
			}
		}

		// Apply the user-provided mapping configurer if necessary
		final BeanProvider beanProvider = buildContext.getServiceManager().getBeanProvider();
		ConfigurationProperty<Optional<HibernateOrmSearchMappingConfigurer>> mappingConfigurerProperty =
				ConfigurationProperty.forKey( SearchOrmSettings.Radicals.MAPPING_CONFIGURER )
						.as(
								HibernateOrmSearchMappingConfigurer.class,
								reference -> beanProvider.getBean( reference, HibernateOrmSearchMappingConfigurer.class )
						)
						.build();
		mappingConfigurerProperty.get( propertySource )
				.ifPresent( configurer -> configurer.configure( this ) );

		super.configure( buildContext, propertySource, configurationCollector );
	}
}
