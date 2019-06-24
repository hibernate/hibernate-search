/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.dsl.query;

import java.util.List;
import java.util.function.Function;

import org.hibernate.search.backend.elasticsearch.search.dsl.predicate.ElasticsearchSearchPredicateFactory;
import org.hibernate.search.backend.elasticsearch.search.dsl.projection.ElasticsearchSearchProjectionFactory;
import org.hibernate.search.engine.search.SearchProjection;
import org.hibernate.search.engine.search.dsl.projection.ProjectionFinalStep;
import org.hibernate.search.engine.search.dsl.query.SearchQueryHitTypeStep;

public interface ElasticsearchSearchQueryHitTypeStep<R, E>
		extends SearchQueryHitTypeStep<
				ElasticsearchSearchQueryOptionsStep<E>,
				R,
				E,
				ElasticsearchSearchProjectionFactory<R, E>,
				ElasticsearchSearchPredicateFactory
		>,
		ElasticsearchSearchQueryPredicateStep<E> {

	@Override
	ElasticsearchSearchQueryPredicateStep<E> asEntity();

	@Override
	ElasticsearchSearchQueryPredicateStep<R> asEntityReference();

	@Override
	<P> ElasticsearchSearchQueryPredicateStep<P> asProjection(
			Function<? super ElasticsearchSearchProjectionFactory<R, E>, ? extends ProjectionFinalStep<P>> projectionContributor);

	@Override
	<P> ElasticsearchSearchQueryPredicateStep<P> asProjection(SearchProjection<P> projection);

	@Override
	ElasticsearchSearchQueryPredicateStep<List<?>> asProjections(SearchProjection<?>... projections);

}
