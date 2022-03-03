/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.projection.dsl;

import java.util.function.Function;

import org.hibernate.search.engine.search.projection.SearchProjection;

/**
 * The second step in a "multi-step" composite projection definition,
 * where 1 projection component has been added and the second component can be added,
 * or the projection can be used as-is.
 *
 * @param <V1> The type of values returned by the first projection component.
 */
public interface CompositeProjectionComponent2Step<V1>
		extends CompositeProjectionComponentsAtLeast1AddedStep {

	@Override
	<V2> CompositeProjectionComponent3Step<V1, V2> add(SearchProjection<V2> projection);

	@Override
	default <V2> CompositeProjectionComponent3Step<V1, V2> add(ProjectionFinalStep<V2> dslFinalStep) {
		return add( dslFinalStep.toProjection() );
	}

	@Override
	<V2, V3> CompositeProjectionComponent4Step<V1, V2, V3> add(SearchProjection<V2> projection1,
			SearchProjection<V3> projection2);

	@Override
	default <V2, V3> CompositeProjectionComponent4Step<V1, V2, V3> add(ProjectionFinalStep<V2> dslFinalStep1,
			ProjectionFinalStep<V3> dslFinalStep2) {
		return add( dslFinalStep1.toProjection(), dslFinalStep2.toProjection() );
	}

	/**
	 * Sets the given function as the way to transform the value of the single projection component added so far.
	 *
	 * @param transformer A function to transform the values of projection components added so far.
	 * @return The final step, where the projection can be retrieved.
	 * @param <V> The type of values returned by the transformer.
	 */
	<V> CompositeProjectionOptionsStep<?, V> transform(Function<V1, V> transformer);

}
