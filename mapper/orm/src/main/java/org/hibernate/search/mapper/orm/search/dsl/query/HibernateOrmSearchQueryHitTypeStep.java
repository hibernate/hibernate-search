/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.search.dsl.query;

import org.hibernate.query.Query;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactory;
import org.hibernate.search.engine.search.dsl.projection.SearchProjectionFactory;
import org.hibernate.search.engine.search.dsl.query.SearchQueryOptionsStep;
import org.hibernate.search.engine.search.dsl.query.SearchQueryHitTypeStep;
import org.hibernate.search.mapper.orm.common.EntityReference;

/**
 * The initial step in a query definition, where the type of query hits can be set,
 * or where the predicate can be set directly, assuming that query hits are returned as entities.
 *
 * @see SearchQueryHitTypeStep
 */
public interface HibernateOrmSearchQueryHitTypeStep<E>
		extends SearchQueryHitTypeStep<
				SearchQueryOptionsStep<?, E, ?>,
				EntityReference,
				E,
				SearchProjectionFactory<EntityReference, E>,
				SearchPredicateFactory
		> {

	/**
	 * Set the JDBC fetch size for this query.
	 *
	 * @param fetchSize The fetch size. Must be positive or zero.
	 * @return {@code this} for method chaining.
	 * @see Query#setFetchSize(int)
	 */
	HibernateOrmSearchQueryHitTypeStep<E> fetchSize(int fetchSize);

}
