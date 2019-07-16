/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.mapper.scope.spi;

import org.hibernate.search.engine.mapper.session.context.spi.SessionContextImplementor;
import org.hibernate.search.engine.search.dsl.aggregation.SearchAggregationFactory;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactory;
import org.hibernate.search.engine.search.dsl.projection.SearchProjectionFactory;
import org.hibernate.search.engine.search.dsl.query.SearchQueryHitTypeStep;
import org.hibernate.search.engine.search.dsl.sort.SearchSortFactory;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContextBuilder;

/**
 * @param <R> The type of entity references, i.e. the type of hits returned by
 * {@link SearchQueryHitTypeStep#asEntityReference() reference queries},
 * or the type of objects returned for {@link SearchProjectionFactory#entityReference() entity reference projections}.
 * @param <E> The type of entities, i.e. the type of hits returned by
 * {@link SearchQueryHitTypeStep#asEntity() entity queries}
 * or the type of objects returned for {@link SearchProjectionFactory#entity() entity projections}.
 */
public interface MappedIndexScope<R, E> {

	/*
	 * IMPLEMENTATION NOTE: we *must* only accept a loading context with the same R/E type parameters as this class,
	 * otherwise some casts in EntityProjectionOptionsStepImpl and EntityReferenceProjectionOptionsStepImpl
	 * will be wrong.
	 * In particular, we cannot accept a LoadingContextBuilder<R, T> with any T.
	 */
	SearchQueryHitTypeStep<?, R, E, SearchProjectionFactory<R, E>, ?> search(
			SessionContextImplementor sessionContext,
			LoadingContextBuilder<R, E> loadingContextBuilder);

	SearchPredicateFactory predicate();

	SearchSortFactory sort();

	/*
	 * IMPLEMENTATION NOTE: we *must* return a factory with the same R/E type arguments as this class,
	 * otherwise some casts in EntityProjectionOptionsStepImpl and EntityReferenceProjectionOptionsStepImpl
	 * will be wrong.
	 */
	SearchProjectionFactory<R, E> projection();

	SearchAggregationFactory aggregation();

}
