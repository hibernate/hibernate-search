/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.projection.dsl;

import org.hibernate.search.engine.search.projection.SearchProjection;

/**
 * A step in a "multi-step" composite projection definition where more components can be added.
 */
public interface CompositeProjectionComponentsAddStep {

	/**
	 * Adds an element to the composite projection based on a previously-built {@link SearchProjection}.
	 *
	 * @param projection The projection to add.
	 * @return A DSL step that can be used to add more values, or to retrieve the projection as-is.
	 * @param <V1> The type of values returned by the projection to add.
	 */
	<V1> CompositeProjectionComponentsAtLeast1AddedStep add(SearchProjection<V1> projection);

	/**
	 * Adds an element to the composite projection based on an almost-built {@link SearchProjection}.
	 *
	 * @param dslFinalStep A final step in the projection DSL allowing the retrieval of a {@link SearchProjection}.
	 * @return A DSL step that can be used to add more values, or to retrieve the projection as-is.
	 * @param <V1> The type of values returned by the projection to add.
	 */
	default <V1> CompositeProjectionComponentsAtLeast1AddedStep add(ProjectionFinalStep<V1> dslFinalStep) {
		return add( dslFinalStep.toProjection() );
	}

	/**
	 * Adds two elements to the composite projection based on previously-built {@link SearchProjection}s.
	 *
	 * @param projection1 The first projection to add.
	 * @param projection2 The second projection to add.
	 * @return A DSL step that can be used to add more values, or to retrieve the projection as-is.
	 * @param <V1> The type of values returned by the first projection to add.
	 * @param <V2> The type of values returned by the second projection to add.
	 */
	<V1, V2> CompositeProjectionComponentsAtLeast1AddedStep add(SearchProjection<V1> projection1,
			SearchProjection<V2> projection2);

	/**
	 * Adds two elements to the composite projection based on almost-built {@link SearchProjection}s.
	 *
	 * @param dslFinalStep1 A final step in the projection DSL allowing the retrieval of the first {@link SearchProjection}.
	 * @param dslFinalStep2 A final step in the projection DSL allowing the retrieval of the second {@link SearchProjection}.
	 * @return A DSL step that can be used to add more values, or to retrieve the projection as-is.
	 * @param <V1> The type of values returned by the first projection to add.
	 * @param <V2> The type of values returned by the second projection to add.
	 */
	default <V1, V2> CompositeProjectionComponentsAtLeast1AddedStep add(ProjectionFinalStep<V1> dslFinalStep1,
			ProjectionFinalStep<V2> dslFinalStep2) {
		return add( dslFinalStep1.toProjection(), dslFinalStep2.toProjection() );
	}

	/**
	 * Adds three elements to the composite projection based on previously-built {@link SearchProjection}s.
	 *
	 * @param projection1 The first projection to add.
	 * @param projection2 The second projection to add.
	 * @param projection3 The third projection to add.
	 * @return A DSL step that can be used to add more values, or to retrieve the projection as-is.
	 * @param <V1> The type of values returned by the first projection to add.
	 * @param <V2> The type of values returned by the second projection to add.
	 * @param <V3> The type of values returned by the third projection to add.
	 */
	<V1, V2, V3> CompositeProjectionComponentsAtLeast1AddedStep add(SearchProjection<V1> projection1,
			SearchProjection<V2> projection2, SearchProjection<V3> projection3);

	/**
	 * Adds three elements to the composite projection based on almost-built {@link SearchProjection}s.
	 *
	 * @param dslFinalStep1 A final step in the projection DSL allowing the retrieval of the first {@link SearchProjection}.
	 * @param dslFinalStep2 A final step in the projection DSL allowing the retrieval of the second {@link SearchProjection}.
	 * @param dslFinalStep3 A final step in the projection DSL allowing the retrieval of the third {@link SearchProjection}.
	 * @return A DSL step that can be used to add more values, or to retrieve the projection as-is.
	 * @param <V1> The type of values returned by the first projection to add.
	 * @param <V2> The type of values returned by the second projection to add.
	 * @param <V3> The type of values returned by the third projection to add.
	 */
	default <V1, V2, V3> CompositeProjectionComponentsAtLeast1AddedStep add(ProjectionFinalStep<V1> dslFinalStep1,
			ProjectionFinalStep<V2> dslFinalStep2, ProjectionFinalStep<V3> dslFinalStep3) {
		return add( dslFinalStep1.toProjection(), dslFinalStep2.toProjection(), dslFinalStep3.toProjection() );
	}

	/**
	 * Adds multiple elements to the composite projection based on previously-built {@link SearchProjection}s.
	 *
	 * @param projections The projection to add, in order.
	 * @return A DSL step that can be used to add more values, or to retrieve the projection as-is.
	 */
	CompositeProjectionComponentsAtLeast1AddedStep add(SearchProjection<?>... projections);

	/**
	 * Adds multiple elements to the composite projection based on the given almost-built {@link SearchProjection}s.
	 *
	 * @param dslFinalSteps The final steps in the projection DSL allowing the retrieval of {@link SearchProjection}s.
	 * @return A DSL step that can be used to add more values, or to retrieve the projection as-is.
	 */
	CompositeProjectionComponentsAtLeast1AddedStep add(ProjectionFinalStep<?>... dslFinalSteps);

}
