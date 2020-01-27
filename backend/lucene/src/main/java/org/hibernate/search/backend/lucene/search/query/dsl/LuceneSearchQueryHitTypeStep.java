/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.query.dsl;

import java.util.List;
import java.util.function.Function;

import org.hibernate.search.backend.lucene.search.predicate.dsl.LuceneSearchPredicateFactory;
import org.hibernate.search.backend.lucene.search.projection.dsl.LuceneSearchProjectionFactory;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.dsl.ProjectionFinalStep;
import org.hibernate.search.engine.search.query.dsl.SearchQueryHitTypeStep;

public interface LuceneSearchQueryHitTypeStep<R, E, LOS>
		extends SearchQueryHitTypeStep<
						LuceneSearchQueryOptionsStep<E, LOS>,
						R,
						E,
						LOS,
						LuceneSearchProjectionFactory<R, E>,
						LuceneSearchPredicateFactory
				>,
				LuceneSearchQueryWhereStep<E, LOS> {

	@Override
	LuceneSearchQueryWhereStep<E, LOS> asEntity();

	@Override
	LuceneSearchQueryWhereStep<R, LOS> asEntityReference();

	@Override
	<P> LuceneSearchQueryWhereStep<P, LOS> asProjection(
			Function<? super LuceneSearchProjectionFactory<R, E>, ? extends ProjectionFinalStep<P>> projectionContributor);

	@Override
	<P> LuceneSearchQueryWhereStep<P, LOS> asProjection(SearchProjection<P> projection);

	@Override
	LuceneSearchQueryWhereStep<List<?>, LOS> asProjections(SearchProjection<?>... projections);

}
