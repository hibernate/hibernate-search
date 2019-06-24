/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.dsl.query;

import java.util.List;
import java.util.function.Function;

import org.hibernate.search.backend.lucene.search.dsl.predicate.LuceneSearchPredicateFactory;
import org.hibernate.search.backend.lucene.search.dsl.projection.LuceneSearchProjectionFactoryContext;
import org.hibernate.search.engine.search.SearchProjection;
import org.hibernate.search.engine.search.dsl.projection.ProjectionFinalStep;
import org.hibernate.search.engine.search.dsl.query.SearchQueryResultDefinitionContext;

public interface LuceneSearchQueryResultDefinitionContext<R, E>
		extends SearchQueryResultDefinitionContext<
				LuceneSearchQueryContext<E>,
				R,
				E,
				LuceneSearchProjectionFactoryContext<R, E>,
				LuceneSearchPredicateFactory
		>,
		LuceneSearchQueryResultContext<E> {

	@Override
	LuceneSearchQueryResultContext<E> asEntity();

	@Override
	LuceneSearchQueryResultContext<R> asEntityReference();

	@Override
	<P> LuceneSearchQueryResultContext<P> asProjection(
			Function<? super LuceneSearchProjectionFactoryContext<R, E>, ? extends ProjectionFinalStep<P>> projectionContributor);

	@Override
	<P> LuceneSearchQueryResultContext<P> asProjection(SearchProjection<P> projection);

	@Override
	LuceneSearchQueryResultContext<List<?>> asProjections(SearchProjection<?>... projections);

}
