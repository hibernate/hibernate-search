/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.projection.dsl.impl;

import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.dsl.CompositeProjectionComponent1Step;
import org.hibernate.search.engine.search.projection.dsl.CompositeProjectionComponent2Step;
import org.hibernate.search.engine.search.projection.dsl.CompositeProjectionComponent3Step;
import org.hibernate.search.engine.search.projection.dsl.CompositeProjectionComponent4Step;
import org.hibernate.search.engine.search.projection.dsl.spi.SearchProjectionDslContext;

public class CompositeProjectionComponent1StepImpl extends AbstractCompositeProjectionComponentsAddStep
		implements CompositeProjectionComponent1Step {

	public CompositeProjectionComponent1StepImpl(SearchProjectionDslContext<?> dslContext) {
		super( dslContext );
	}

	@Override
	public <V1> CompositeProjectionComponent2Step<V1> add(SearchProjection<V1> projection) {
		return new CompositeProjectionComponent2StepImpl<>( dslContext, projection );
	}

	@Override
	public <V1, V2> CompositeProjectionComponent3Step<V1, V2> add(SearchProjection<V1> projection1,
			SearchProjection<V2> projection2) {
		return new CompositeProjectionComponent3StepImpl<>( dslContext, projection1, projection2 );
	}

	@Override
	public <V1, V2, V3> CompositeProjectionComponent4Step<V1, V2, V3> add(SearchProjection<V1> projection1,
			SearchProjection<V2> projection2, SearchProjection<V3> projection3) {
		return new CompositeProjectionComponent4StepImpl<>( dslContext, projection1, projection2, projection3 );
	}

	@Override
	SearchProjection<?>[] toProjectionArray(SearchProjection<?>... otherProjections) {
		return otherProjections;
	}
}
