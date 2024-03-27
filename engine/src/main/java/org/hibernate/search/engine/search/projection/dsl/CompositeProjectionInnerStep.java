/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.projection.dsl;

import org.hibernate.search.engine.search.projection.SearchProjection;

/**
 * A step in a "multi-step" composite projection definition
 * where one can define inner projections to get values from.
 */
public interface CompositeProjectionInnerStep {

	/**
	 * Defines the result of the composite projection
	 * as the result of applying the given function to the single inner projection defined so far.
	 *
	 * @param objectClass The type of objects returned by the projection.
	 * The class is expected to be mapped (generally through annotations)
	 * in such a way that it defines the inner projections.
	 * @return The next DSL step.
	 * @param <V> The type of objects returned by the projection.
	 */
	<V> CompositeProjectionValueStep<?, V> as(Class<V> objectClass);

	/**
	 * Defines one inner projection to get values from,
	 * based on a previously-built {@link SearchProjection}.
	 *
	 * @param projection The inner projection.
	 * @return The next DSL step.
	 * @param <V1> The type of values returned by the inner projection.
	 */
	<V1> CompositeProjectionFrom1AsStep<V1> from(SearchProjection<V1> projection);

	/**
	 * Defines one inner projection to get values from,
	 * based on an almost-built {@link SearchProjection}.
	 *
	 * @param dslFinalStep A final step in the projection DSL allowing the retrieval of the inner projection.
	 * @return The next DSL step.
	 * @param <V1> The type of values returned by the inner projection.
	 */
	default <V1> CompositeProjectionFrom1AsStep<V1> from(ProjectionFinalStep<V1> dslFinalStep) {
		return from( dslFinalStep.toProjection() );
	}

	/**
	 * Defines two inner projections to get values from,
	 * based on previously-built {@link SearchProjection}s.
	 *
	 * @param projection1 The first inner projection.
	 * @param projection2 The second inner projection.
	 * @return The next DSL step.
	 * @param <V1> The type of values returned by the first inner projection.
	 * @param <V2> The type of values returned by the second inner projection.
	 */
	<V1, V2> CompositeProjectionFrom2AsStep<V1, V2> from(SearchProjection<V1> projection1,
			SearchProjection<V2> projection2);

	/**
	 * Defines two inner projections to get values from,
	 * based on almost-built {@link SearchProjection}s.
	 *
	 * @param dslFinalStep1 A final step in the projection DSL allowing the retrieval of the first inner projection.
	 * @param dslFinalStep2 A final step in the projection DSL allowing the retrieval of the second inner projection.
	 * @return The next DSL step.
	 * @param <V1> The type of values returned by the first inner projection.
	 * @param <V2> The type of values returned by the second inner projection.
	 */
	default <V1, V2> CompositeProjectionFrom2AsStep<V1, V2> from(ProjectionFinalStep<V1> dslFinalStep1,
			ProjectionFinalStep<V2> dslFinalStep2) {
		return from( dslFinalStep1.toProjection(), dslFinalStep2.toProjection() );
	}

	/**
	 * Defines three inner projections to get values from,
	 * based on previously-built {@link SearchProjection}s.
	 *
	 * @param projection1 The first inner projection.
	 * @param projection2 The second inner projection.
	 * @param projection3 The third inner projection.
	 * @return The next DSL step.
	 * @param <V1> The type of values returned by the first inner projection.
	 * @param <V2> The type of values returned by the second inner projection.
	 * @param <V3> The type of values returned by the third inner projection.
	 */
	<V1, V2, V3> CompositeProjectionFrom3AsStep<V1, V2, V3> from(SearchProjection<V1> projection1,
			SearchProjection<V2> projection2, SearchProjection<V3> projection3);

	/**
	 * Defines three inner projections to get values from,
	 * based on almost-built {@link SearchProjection}s.
	 *
	 * @param dslFinalStep1 A final step in the projection DSL allowing the retrieval of the first inner projection.
	 * @param dslFinalStep2 A final step in the projection DSL allowing the retrieval of the second inner projection.
	 * @param dslFinalStep3 A final step in the projection DSL allowing the retrieval of the third inner projection.
	 * @return The next DSL step.
	 * @param <V1> The type of values returned by the first inner projection.
	 * @param <V2> The type of values returned by the second inner projection.
	 * @param <V3> The type of values returned by the third inner projection.
	 */
	default <V1, V2, V3> CompositeProjectionFrom3AsStep<V1, V2, V3> from(ProjectionFinalStep<V1> dslFinalStep1,
			ProjectionFinalStep<V2> dslFinalStep2, ProjectionFinalStep<V3> dslFinalStep3) {
		return from( dslFinalStep1.toProjection(), dslFinalStep2.toProjection(), dslFinalStep3.toProjection() );
	}

	/**
	 * Defines multiple inner projections to get values from,
	 * based on previously-built {@link SearchProjection}s.
	 *
	 * @param projections The inner projections, in order.
	 * @return The next DSL step.
	 */
	CompositeProjectionFromAsStep from(SearchProjection<?>... projections);

	/**
	 * Defines multiple inner projections to get values from,
	 * based on almost-built {@link SearchProjection}s.
	 *
	 * @param dslFinalSteps The final steps in the projection DSL allowing the retrieval of inner projections, in order.
	 * @return The next DSL step.
	 */
	CompositeProjectionFromAsStep from(ProjectionFinalStep<?>... dslFinalSteps);

}
