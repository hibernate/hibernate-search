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
import org.hibernate.search.engine.mapper.mapping.building.spi.MetadataCollector;
import org.hibernate.search.mapper.orm.cfg.SearchOrmSettings;
import org.hibernate.search.mapper.orm.mapping.impl.HibernateOrmMappingFactory;
import org.hibernate.search.mapper.orm.mapping.impl.HibernateOrmMappingKey;
import org.hibernate.search.mapper.orm.mapping.impl.HibernateOrmMetatadaContributor;
import org.hibernate.search.mapper.orm.model.impl.HibernateOrmBootstrapIntrospector;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingContributorImpl;

/*
 * TODO create a Hibernate ORM specific mapper, with the following additions:
 *  1. During mapping creation, use the Hibernate ORM identifier as a fallback when no document ID was found
 *  2. Save additional information regarding containedIn, and make it available in the mapping
 *  3. Use a specific introspector that will comply with Hibernate ORM's access mode
 *  4. When the @DocumentId is the @Id, use the provided ID in priority and only if it's missing, unproxy the entity and get the ID;
 *     when the @DocumentId is NOT the @Id, always ignore the provided ID. See org.hibernate.search.engine.common.impl.WorkPlan.PerClassWork.extractProperId(Work)
 *  5. And more?
 */
public class HibernateOrmMappingContributor extends PojoMappingContributorImpl<HibernateOrmMapping> {

	public static HibernateOrmMappingContributor create(SearchMappingRepositoryBuilder mappingRepositoryBuilder,
			Metadata metadata,
			SessionFactoryImplementor sessionFactoryImplementor,
			boolean annotatedTypeDiscoveryEnabled) {
		HibernateOrmBootstrapIntrospector introspector =
				new HibernateOrmBootstrapIntrospector( metadata, sessionFactoryImplementor );

		return new HibernateOrmMappingContributor(
				mappingRepositoryBuilder, metadata,
				introspector, sessionFactoryImplementor,
				annotatedTypeDiscoveryEnabled
		);
	}

	private HibernateOrmMappingContributor(SearchMappingRepositoryBuilder mappingRepositoryBuilder,
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
		addMetadataContributor(
				new HibernateOrmMetatadaContributor( getMapperFactory(), introspector, metadata )
		);
	}

	@Override
	public void contribute(BuildContext buildContext, ConfigurationPropertySource propertySource,
			MetadataCollector collector) {
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

		super.contribute( buildContext, propertySource, collector );
	}
}
