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
import org.hibernate.search.backend.lucene.search.dsl.projection.LuceneSearchProjectionFactory;
import org.hibernate.search.engine.search.SearchProjection;
import org.hibernate.search.engine.search.dsl.projection.ProjectionFinalStep;
import org.hibernate.search.engine.search.dsl.query.SearchQueryHitTypeStep;

public interface LuceneSearchQueryHitTypeStep<R, E>
		extends SearchQueryHitTypeStep<
		LuceneSearchQueryOptionsStep<E>,
				R,
				E,
				LuceneSearchProjectionFactory<R, E>,
				LuceneSearchPredicateFactory
		>,
		LuceneSearchQueryPredicateStep<E> {

	@Override
	LuceneSearchQueryPredicateStep<E> asEntity();

	@Override
	LuceneSearchQueryPredicateStep<R> asEntityReference();

	@Override
	<P> LuceneSearchQueryPredicateStep<P> asProjection(
			Function<? super LuceneSearchProjectionFactory<R, E>, ? extends ProjectionFinalStep<P>> projectionContributor);

	@Override
	<P> LuceneSearchQueryPredicateStep<P> asProjection(SearchProjection<P> projection);

	@Override
	LuceneSearchQueryPredicateStep<List<?>> asProjections(SearchProjection<?>... projections);

}
