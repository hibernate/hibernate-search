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
				ElasticsearchSearchQueryOptionsStep<E, E, LOS>,
				R,
				E,
				LOS,
				ElasticsearchSearchProjectionFactory<R, E>,
				ElasticsearchSearchPredicateFactory<E>>,
		ElasticsearchSearchQueryWhereStep<E, E, LOS> {

	@Override
	ElasticsearchSearchQueryWhereStep<E, E, LOS> selectEntity();

	@Override
	ElasticsearchSearchQueryWhereStep<E, R, LOS> selectEntityReference();

	@Override
	<P> ElasticsearchSearchQueryWhereStep<E, P, LOS> select(Class<P> objectClass);

	@Override
	<P> ElasticsearchSearchQueryWhereStep<E, P, LOS> select(
			Function<? super ElasticsearchSearchProjectionFactory<R, E>,
					? extends ProjectionFinalStep<P>> projectionContributor);

	@Override
	<P> ElasticsearchSearchQueryWhereStep<E, P, LOS> select(SearchProjection<P> projection);

	@Override
	ElasticsearchSearchQueryWhereStep<E, List<?>, LOS> select(SearchProjection<?>... projections);

}
