/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.projection.dsl.impl;

import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.dsl.CompositeProjectionComponentsAddStep;
import org.hibernate.search.engine.search.projection.dsl.CompositeProjectionComponentsAtLeast1AddedStep;
import org.hibernate.search.engine.search.projection.dsl.ProjectionFinalStep;
import org.hibernate.search.engine.search.projection.dsl.spi.SearchProjectionDslContext;
import org.hibernate.search.util.common.impl.Contracts;

abstract class AbstractCompositeProjectionComponentsAddStep
		implements CompositeProjectionComponentsAddStep {

	final SearchProjectionDslContext<?> dslContext;

	public AbstractCompositeProjectionComponentsAddStep(SearchProjectionDslContext<?> dslContext) {
		this.dslContext = dslContext;
	}

	@Override
	public <V1> CompositeProjectionComponentsAtLeast1AddedStep add(SearchProjection<V1> projection) {
		return add( new SearchProjection<?>[]{ projection } );
	}

	@Override
	public <V1, V2> CompositeProjectionComponentsAtLeast1AddedStep add(SearchProjection<V1> projection1,
			SearchProjection<V2> projection2) {
		return add( new SearchProjection<?>[]{ projection1, projection2 } );
	}

	@Override
	public <V1, V2, V3> CompositeProjectionComponentsAtLeast1AddedStep add(SearchProjection<V1> projection1,
			SearchProjection<V2> projection2, SearchProjection<V3> projection3) {
		return add( new SearchProjection<?>[]{ projection1, projection2, projection3 } );
	}

	@Override
	public CompositeProjectionComponentsAtLeast1AddedStep add(SearchProjection<?>... projections) {
		Contracts.assertNotNullNorEmpty( projections, "projections" );
		return new CompositeProjectionComponentUnknownNumberAddedStep( dslContext, toProjectionArray( projections ) );
	}

	@Override
	public final CompositeProjectionComponentsAtLeast1AddedStep add(ProjectionFinalStep<?>... dslFinalSteps) {
		Contracts.assertNotNullNorEmpty( dslFinalSteps, "dslFinalSteps" );
		SearchProjection<?>[] projections = new SearchProjection<?>[dslFinalSteps.length];
		for ( int i = 0; i < dslFinalSteps.length; i++ ) {
			projections[i] = dslFinalSteps[i].toProjection();
		}
		return add( projections );
	}

	abstract SearchProjection<?>[] toProjectionArray(SearchProjection<?>... otherProjections);

}
