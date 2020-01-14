/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.search.query.dsl;

import org.hibernate.query.Query;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.engine.search.query.dsl.SearchQueryOptionsStep;
import org.hibernate.search.engine.search.query.dsl.SearchQueryHitTypeStep;
import org.hibernate.search.mapper.orm.common.EntityReference;
import org.hibernate.search.mapper.orm.search.loading.EntityLoadingCacheLookupStrategy;
import org.hibernate.search.mapper.orm.search.loading.dsl.SearchLoadingOptionsStep;

/**
 * The initial step in a query definition, where the type of query hits can be set,
 * or where the predicate can be set directly, assuming that query hits are returned as entities.
 *
 * @see SearchQueryHitTypeStep
 * @deprecated Use {@link SearchQueryHitTypeStep} instead.
 */
@Deprecated
public interface HibernateOrmSearchQueryHitTypeStep<E>
		extends SearchQueryHitTypeStep<
						SearchQueryOptionsStep<?, E, SearchLoadingOptionsStep, ?, ?>,
						EntityReference,
						E,
						SearchLoadingOptionsStep,
						SearchProjectionFactory<EntityReference, E>,
						SearchPredicateFactory
				> {

	/**
	 * Set the JDBC fetch size for this query.
	 *
	 * @param fetchSize The fetch size. Must be positive or zero.
	 * @return {@code this} for method chaining.
	 * @see Query#setFetchSize(int)
	 * @deprecated Call {@code .loading( o -> o.fetchSize( ... )} near the end of the query definition instead.
	 */
	@Deprecated
	HibernateOrmSearchQueryHitTypeStep<E> fetchSize(int fetchSize);

	/**
	 * Set the strategy for cache lookup before query results are loaded.
	 *
	 * @param strategy The strategy.
	 * @return {@code this} for method chaining.
	 * @deprecated Call {@code .loading( o -> o.cacheLookupStrategy( ... )} near the end of the query definition instead.
	 */
	@Deprecated
	HibernateOrmSearchQueryHitTypeStep<E> cacheLookupStrategy(EntityLoadingCacheLookupStrategy strategy);

}
