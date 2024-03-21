/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.sort.dsl;

import java.util.function.Function;

import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.spatial.GeoPoint;

/**
 * The initial step in a "distance" sort definition, where center point to calculate the distance from must be set.
 *
 * @param <S> The "self" type (the actual exposed type of this step).
 * @param <PDF> The type of factory used to create predicates in {@link DistanceSortOptionsStep#filter(Function)}.
 */
public interface DistanceSortFromStep<S extends DistanceSortFromStep<?, PDF>, PDF extends SearchPredicateFactory> {

	/**
	 * Defines the center from which the distance is computed from.
	 *
	 * @param center The center to compute the distance from.
	 * @return A new step to define optional parameters for the distance projection.
	 */
	DistanceSortOptionsStep<?, ? extends SearchPredicateFactory> from(GeoPoint center);

	/**
	 * Defines the center from which the distance is computed from.
	 *
	 * @param parameterName The name of a query parameter representing the center to compute the distance from.
	 * @return A new step to define optional parameters for the distance projection.
	 */
	DistanceSortOptionsStep<?, ? extends SearchPredicateFactory> fromParam(String parameterName);

}
