/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.projection.dsl;

import java.util.function.BiFunction;

import org.hibernate.search.engine.search.projection.SearchProjection;

/**
 * The third step in a "multi-step" composite projection definition,
 * where 2 projection components have been added and the third component can be added,
 * or the projection can be used as-is.
 *
 * @param <V1> The type of values returned by the first projection component.
 * @param <V2> The type of values returned by the second projection component.
 */
public interface CompositeProjectionComponent3Step<V1, V2>
		extends CompositeProjectionComponentsAtLeast1AddedStep {

	@Override
	<V3> CompositeProjectionComponent4Step<V1, V2, V3> add(SearchProjection<V3> projection);

	@Override
	default <V3> CompositeProjectionComponent4Step<V1, V2, V3> add(ProjectionFinalStep<V3> dslFinalStep) {
		return add( dslFinalStep.toProjection() );
	}

	/**
	 * Sets the given function as the way to transform the values of projection components added so far,
	 * combining them into a single value.
	 *
	 * @param transformer A function to transform the values of projection components added so far.
	 * @return The final step, where the projection can be retrieved.
	 * @param <V> The type of values returned by the transformer.
	 */
	<V> CompositeProjectionOptionsStep<?, V> transform(BiFunction<V1, V2, V> transformer);

}
