/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.spi;

import org.hibernate.search.engine.search.SearchPredicate;

/**
 * A factory for search predicates.
 * <p>
 * This is the main entry point for the engine
 * to ask the backend to build search predicates.
 *
 * @param <CTX> The type of the context passed to the contribution method.
 * @param <C> The type of predicate collector the builders contributor will contribute to.
 * This type is backend-specific. See {@link SearchPredicateBuilder#contribute(Object, Object)}
 */
public interface SearchPredicateFactory<CTX, C> {

	CTX createRootContext();

	SearchPredicate toSearchPredicate(SearchPredicateContributor<CTX, ? super C> contributor);

	SearchPredicateContributor<CTX, C> toContributor(SearchPredicate predicate);

	MatchAllPredicateBuilder<CTX, C> matchAll();

	BooleanJunctionPredicateBuilder<CTX, C> bool();

	MatchPredicateBuilder<CTX, C> match(String absoluteFieldPath);

	RangePredicateBuilder<CTX, C> range(String absoluteFieldPath);

	NestedPredicateBuilder<CTX, C> nested(String absoluteFieldPath);

	SpatialWithinCirclePredicateBuilder<CTX, C> spatialWithinCircle(String absoluteFieldPath);

	SpatialWithinPolygonPredicateBuilder<CTX, C> spatialWithinPolygon(String absoluteFieldPath);

	SpatialWithinBoundingBoxPredicateBuilder<CTX, C> spatialWithinBoundingBox(String absoluteFieldPath);

}
