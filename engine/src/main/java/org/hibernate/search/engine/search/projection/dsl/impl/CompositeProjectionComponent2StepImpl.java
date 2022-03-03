/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.projection.dsl.impl;

import java.util.function.Function;

import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.dsl.CompositeProjectionComponent2Step;
import org.hibernate.search.engine.search.projection.dsl.CompositeProjectionComponent3Step;
import org.hibernate.search.engine.search.projection.dsl.CompositeProjectionComponent4Step;
import org.hibernate.search.engine.search.projection.dsl.CompositeProjectionOptionsStep;
import org.hibernate.search.engine.search.projection.dsl.spi.SearchProjectionDslContext;

class CompositeProjectionComponent2StepImpl<V1> extends AbstractCompositeProjectionComponentsAtLeastOneAddedStep
		implements CompositeProjectionComponent2Step<V1> {

	final SearchProjection<V1> component1;

	public CompositeProjectionComponent2StepImpl(SearchProjectionDslContext<?> dslContext,
			SearchProjection<V1> component1) {
		super( dslContext );
		this.component1 = component1;
	}

	@Override
	public <V2> CompositeProjectionComponent3Step<V1, V2> add(SearchProjection<V2> projection) {
		return new CompositeProjectionComponent3StepImpl<>( dslContext, component1, projection );
	}

	@Override
	public <V2, V3> CompositeProjectionComponent4Step<V1, V2, V3> add(SearchProjection<V2> projection1,
			SearchProjection<V3> projection2) {
		return new CompositeProjectionComponent4StepImpl<>( dslContext, component1, projection1, projection2 );
	}

	@Override
	public <V> CompositeProjectionOptionsStep<?, V> transform(Function<V1, V> transformer) {
		return new CompositeProjectionFinalStep<>( dslContext, transformer, component1 );
	}

	@Override
	SearchProjection<?>[] toProjectionArray(SearchProjection<?>... otherProjections) {
		SearchProjection<?>[] array = new SearchProjection[1 + otherProjections.length];
		array[0] = component1;
		System.arraycopy( otherProjections, 0, array, 1, otherProjections.length );
		return array;
	}

	@Override
	SearchProjection<?>[] toProjectionArray() {
		return new SearchProjection<?>[] { component1 };
	}
}
