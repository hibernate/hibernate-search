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
import org.hibernate.search.engine.search.query.dsl.SearchQuerySelectStep;

public interface ElasticsearchSearchQuerySelectStep<R, E, LOS>
		extends SearchQuerySelectStep<
						ElasticsearchSearchQueryOptionsStep<E, LOS>,
						R,
						E,
						LOS,
						ElasticsearchSearchProjectionFactory<R, E>,
						ElasticsearchSearchPredicateFactory
				>,
				ElasticsearchSearchQueryWhereStep<E, LOS> {

	@Override
	ElasticsearchSearchQueryWhereStep<E, LOS> selectEntity();

	@Override
	ElasticsearchSearchQueryWhereStep<R, LOS> selectEntityReference();

	@Override
	<P> ElasticsearchSearchQueryWhereStep<P, LOS> select(
			Function<? super ElasticsearchSearchProjectionFactory<R, E>, ? extends ProjectionFinalStep<P>> projectionContributor);

	@Override
	<P> ElasticsearchSearchQueryWhereStep<P, LOS> select(SearchProjection<P> projection);

	@Override
	ElasticsearchSearchQueryWhereStep<List<?>, LOS> select(SearchProjection<?>... projections);

	@Override
	@Deprecated
	default ElasticsearchSearchQueryWhereStep<E, ?> asEntity() {
		return selectEntity();
	}

	@Override
	@Deprecated
	default ElasticsearchSearchQueryWhereStep<R, ?> asEntityReference() {
		return selectEntityReference();
	}

	@Override
	@Deprecated
	default <P> ElasticsearchSearchQueryWhereStep<P, ?> asProjection(
			Function<? super ElasticsearchSearchProjectionFactory<R, E>, ? extends ProjectionFinalStep<P>> projectionContributor) {
		return select( projectionContributor );
	}

	@Override
	@Deprecated
	default <P> ElasticsearchSearchQueryWhereStep<P, ?> asProjection(SearchProjection<P> projection) {
		return select( projection );
	}

	@Override
	@Deprecated
	default ElasticsearchSearchQueryWhereStep<List<?>, ?> asProjections(SearchProjection<?>... projections) {
		return select( projections );
	}

}
