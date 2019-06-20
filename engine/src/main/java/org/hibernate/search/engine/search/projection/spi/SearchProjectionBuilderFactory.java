/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.projection.spi;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.hibernate.search.engine.search.SearchProjection;
import org.hibernate.search.engine.search.projection.ProjectionConverter;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.common.function.TriFunction;

/**
 * A factory for search projection builders.
 * <p>
 * This is the main entry point for the engine
 * to ask the backend to build search projections.
 */
public interface SearchProjectionBuilderFactory {

	DocumentReferenceProjectionBuilder documentReference();

	<T> FieldProjectionBuilder<T> field(String absoluteFieldPath, Class<T> clazz, ProjectionConverter projectionConverter);

	<E> EntityProjectionBuilder<E> entity();

	<R> EntityReferenceProjectionBuilder<R> entityReference();

	ScoreProjectionBuilder score();

	DistanceToFieldProjectionBuilder distance(String absoluteFieldPath, GeoPoint center);

	<P> CompositeProjectionBuilder<P> composite(Function<List<?>, P> transformer, SearchProjection<?>... projections);

	<P1, P> CompositeProjectionBuilder<P> composite(Function<P1, P> transformer, SearchProjection<P1> projection);

	<P1, P2, P> CompositeProjectionBuilder<P> composite(BiFunction<P1, P2, P> transformer,
			SearchProjection<P1> projection1, SearchProjection<P2> projection2);

	<P1, P2, P3, P> CompositeProjectionBuilder<P> composite(TriFunction<P1, P2, P3, P> transformer,
			SearchProjection<P1> projection1, SearchProjection<P2> projection2, SearchProjection<P3> projection3);
}
