/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.mapping;

import org.hibernate.SessionFactory;
import org.hibernate.search.engine.common.SearchMappingRepositoryBuilder;
import org.hibernate.search.mapper.orm.mapping.impl.HibernateOrmMapperFactory;
import org.hibernate.search.mapper.orm.mapping.impl.HibernateOrmMappingImpl;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingContributorImpl;

/**
 * @author Yoann Rodiere
 */
/*
 * TODO create a Hibernate ORM specific mapper, with the following additions:
 *  1. When processing annotations, use @Id as a fallback when no document ID was found
 *  2. Save additional information regarding containedIn, and make it available in the mapping
 *  3. Use a specific introspector that will comply with Hibernate ORM's access mode
 *  4. When the @DocumentId is the @Id, use the provided ID in priority and only if it's missing, unproxy the entity and get the ID;
 *     when the @DocumentId is NOT the @Id, always ignore the provided ID. See org.hibernate.search.engine.common.impl.WorkPlan.PerClassWork.extractProperId(Work)
 *  5. And more?
 */
public class HibernateOrmMappingContributor extends PojoMappingContributorImpl<HibernateOrmMapping, HibernateOrmMappingImpl> {

	public HibernateOrmMappingContributor(SearchMappingRepositoryBuilder mappingRepositoryBuilder,
			SessionFactory sessionFactory) {
		super( mappingRepositoryBuilder, new HibernateOrmMapperFactory( sessionFactory ) );
	}

	@Override
	protected HibernateOrmMapping toReturnType(HibernateOrmMappingImpl mapping) {
		return mapping;
	}
}
