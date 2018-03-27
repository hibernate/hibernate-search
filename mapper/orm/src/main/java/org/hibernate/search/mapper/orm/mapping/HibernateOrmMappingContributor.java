/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.mapping;

import org.hibernate.boot.Metadata;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.search.engine.common.SearchMappingRepositoryBuilder;
import org.hibernate.search.mapper.orm.mapping.impl.HibernateOrmMappingFactory;
import org.hibernate.search.mapper.orm.mapping.impl.HibernateOrmMappingKey;
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

	public HibernateOrmMappingContributor(SearchMappingRepositoryBuilder mappingRepositoryBuilder,
			Metadata metadata, SessionFactoryImplementor sessionFactoryImplementor,
			boolean annotatedTypeDiscoveryEnabled) {
		this( mappingRepositoryBuilder, new HibernateOrmBootstrapIntrospector( metadata, sessionFactoryImplementor ),
				sessionFactoryImplementor, annotatedTypeDiscoveryEnabled );
	}

	private HibernateOrmMappingContributor(SearchMappingRepositoryBuilder mappingRepositoryBuilder,
			HibernateOrmBootstrapIntrospector introspector, SessionFactoryImplementor sessionFactoryImplementor,
			boolean annotatedTypeDiscoveryEnabled) {
		super(
				mappingRepositoryBuilder, new HibernateOrmMappingKey(),
				new HibernateOrmMappingFactory( sessionFactoryImplementor ),
				introspector, annotatedTypeDiscoveryEnabled
		);
	}

}
