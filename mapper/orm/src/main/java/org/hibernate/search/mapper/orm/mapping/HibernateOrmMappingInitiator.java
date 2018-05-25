/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.mapping;

import java.util.Optional;

import org.hibernate.MultiTenancyStrategy;
import org.hibernate.boot.Metadata;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.common.SearchMappingRepositoryBuilder;
import org.hibernate.search.engine.common.spi.BeanProvider;
import org.hibernate.search.engine.common.spi.BuildContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingConfigurationCollector;
import org.hibernate.search.mapper.orm.cfg.SearchOrmSettings;
import org.hibernate.search.mapper.orm.mapping.impl.HibernateOrmMappingFactory;
import org.hibernate.search.mapper.orm.mapping.impl.HibernateOrmMappingKey;
import org.hibernate.search.mapper.orm.mapping.impl.HibernateOrmMetatadaContributor;
import org.hibernate.search.mapper.orm.model.impl.HibernateOrmBootstrapIntrospector;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoTypeMetadataContributor;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingInitiatorImpl;

/*
 * TODO make the following additions to the Hibernate ORM specific mapping:
 *  1. During mapping creation, use the Hibernate ORM identifier as a fallback when no document ID was found
 *  2. When the @DocumentId is the @Id, use the provided ID in priority and only if it's missing, unproxy the entity and get the ID;
 *     when the @DocumentId is NOT the @Id, always ignore the provided ID. See org.hibernate.search.engine.common.impl.WorkPlan.PerClassWork.extractProperId(Work)
 *  3. And more?
 */
public class HibernateOrmMappingInitiator extends PojoMappingInitiatorImpl<HibernateOrmMapping> {

	public static HibernateOrmMappingInitiator create(SearchMappingRepositoryBuilder mappingRepositoryBuilder,
			Metadata metadata,
			SessionFactoryImplementor sessionFactoryImplementor,
			boolean annotatedTypeDiscoveryEnabled) {
		HibernateOrmBootstrapIntrospector introspector =
				new HibernateOrmBootstrapIntrospector( metadata, sessionFactoryImplementor );

		return new HibernateOrmMappingInitiator(
				mappingRepositoryBuilder, metadata,
				introspector, sessionFactoryImplementor,
				annotatedTypeDiscoveryEnabled
		);
	}

	private HibernateOrmMappingInitiator(SearchMappingRepositoryBuilder mappingRepositoryBuilder,
			Metadata metadata,
			HibernateOrmBootstrapIntrospector introspector,
			SessionFactoryImplementor sessionFactoryImplementor,
			boolean annotatedTypeDiscoveryEnabled) {
		super(
				mappingRepositoryBuilder, new HibernateOrmMappingKey(),
				new HibernateOrmMappingFactory( sessionFactoryImplementor ),
				introspector, false,
				annotatedTypeDiscoveryEnabled,
				!MultiTenancyStrategy.NONE.equals( sessionFactoryImplementor.getSessionFactoryOptions().getMultiTenancyStrategy() )
		);
		addConfigurationContributor(
				new HibernateOrmMetatadaContributor( introspector, metadata )
		);
	}

	@Override
	public void configure(BuildContext buildContext, ConfigurationPropertySource propertySource,
			MappingConfigurationCollector<PojoTypeMetadataContributor> configurationCollector) {
		// Apply the user-provided metadata contributor if necessary
		final BeanProvider beanProvider = buildContext.getServiceManager().getBeanProvider();
		ConfigurationProperty<Optional<HibernateOrmSearchMappingContributor>> userMappingContributorProperty =
				ConfigurationProperty.forKey( SearchOrmSettings.Radicals.MAPPING_CONTRIBUTOR )
						.as(
								HibernateOrmSearchMappingContributor.class,
								reference -> beanProvider.getBean( reference, HibernateOrmSearchMappingContributor.class )
						)
						.build();
		userMappingContributorProperty.get( propertySource )
				.ifPresent( userContributor -> userContributor.contribute( this ) );

		super.configure( buildContext, propertySource, configurationCollector );
	}
}
