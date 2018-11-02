/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.search;

import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactoryContext;
import org.hibernate.search.engine.search.dsl.projection.SearchProjectionFactoryContext;
import org.hibernate.search.engine.search.dsl.query.SearchQueryResultDefinitionContext;
import org.hibernate.search.engine.search.dsl.sort.SearchSortContainerContext;
import org.hibernate.search.mapper.orm.hibernate.HibernateOrmSearchQueryResultDefinitionContext;
import org.hibernate.search.mapper.pojo.search.PojoReference;

public interface HibernateOrmSearchTarget<T> {

	HibernateOrmSearchQueryResultDefinitionContext<T> jpaQuery();

	SearchQueryResultDefinitionContext<PojoReference, T> query();

	SearchPredicateFactoryContext predicate();

	SearchSortContainerContext sort();

	SearchProjectionFactoryContext projection();

}
