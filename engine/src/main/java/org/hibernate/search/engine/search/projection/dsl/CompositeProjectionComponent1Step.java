/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.projection.dsl;

import org.hibernate.search.engine.search.projection.SearchProjection;

/**
 * The initial step in a "multi-step" composite projection definition,
 * where no components have been added yet and the first component can be added.
 */
public interface CompositeProjectionComponent1Step
		extends CompositeProjectionComponentsAddStep {

	@Override
	<V1> CompositeProjectionComponent2Step<V1> add(SearchProjection<V1> projection);

	@Override
	default <V1> CompositeProjectionComponent2Step<V1> add(ProjectionFinalStep<V1> dslFinalStep) {
		return add( dslFinalStep.toProjection() );
	}

	@Override
	<V1, V2> CompositeProjectionComponent3Step<V1, V2> add(SearchProjection<V1> projection1,
			SearchProjection<V2> projection2);

	@Override
	default <V1, V2> CompositeProjectionComponent3Step<V1, V2> add(ProjectionFinalStep<V1> dslFinalStep1,
			ProjectionFinalStep<V2> dslFinalStep2) {
		return add( dslFinalStep1.toProjection(), dslFinalStep2.toProjection() );
	}

	@Override
	<V1, V2, V3> CompositeProjectionComponent4Step<V1, V2, V3> add(SearchProjection<V1> projection1,
			SearchProjection<V2> projection2, SearchProjection<V3> projection3);

	@Override
	default <V1, V2, V3> CompositeProjectionComponent4Step<V1, V2, V3> add(ProjectionFinalStep<V1> dslFinalStep1,
			ProjectionFinalStep<V2> dslFinalStep2, ProjectionFinalStep<V3> dslFinalStep3) {
		return add( dslFinalStep1.toProjection(), dslFinalStep2.toProjection(), dslFinalStep3.toProjection() );
	}

}
