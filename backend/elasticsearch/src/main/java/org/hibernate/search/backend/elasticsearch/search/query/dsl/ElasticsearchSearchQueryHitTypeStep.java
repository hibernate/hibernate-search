/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.query.dsl;

import java.util.List;
import java.util.function.Function;

import org.hibernate.search.backend.elasticsearch.search.predicate.dsl.ElasticsearchSearchPredicateFactory;
import org.hibernate.search.backend.elasticsearch.search.projection.dsl.ElasticsearchSearchProjectionFactory;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.dsl.ProjectionFinalStep;
import org.hibernate.search.engine.search.query.dsl.SearchQueryHitTypeStep;

public interface ElasticsearchSearchQueryHitTypeStep<R, E, LOS>
		extends SearchQueryHitTypeStep<
				ElasticsearchSearchQueryOptionsStep<E, LOS>,
				R,
				E,
				LOS,
				ElasticsearchSearchProjectionFactory<R, E>,
				ElasticsearchSearchPredicateFactory
		>,
		ElasticsearchSearchQueryPredicateStep<E, LOS> {

	@Override
	ElasticsearchSearchQueryPredicateStep<E, LOS> asEntity();

	@Override
	ElasticsearchSearchQueryPredicateStep<R, LOS> asEntityReference();

	@Override
	<P> ElasticsearchSearchQueryPredicateStep<P, LOS> asProjection(
			Function<? super ElasticsearchSearchProjectionFactory<R, E>, ? extends ProjectionFinalStep<P>> projectionContributor);

	@Override
	<P> ElasticsearchSearchQueryPredicateStep<P, LOS> asProjection(SearchProjection<P> projection);

	@Override
	ElasticsearchSearchQueryPredicateStep<List<?>, LOS> asProjections(SearchProjection<?>... projections);

}
