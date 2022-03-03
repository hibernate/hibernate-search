/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.projection.dsl.impl;

import java.util.function.BiFunction;

import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.dsl.CompositeProjectionComponent3Step;
import org.hibernate.search.engine.search.projection.dsl.CompositeProjectionComponent4Step;
import org.hibernate.search.engine.search.projection.dsl.CompositeProjectionOptionsStep;
import org.hibernate.search.engine.search.projection.dsl.spi.SearchProjectionDslContext;

class CompositeProjectionComponent3StepImpl<V1, V2>
		extends AbstractCompositeProjectionComponentsAtLeastOneAddedStep
		implements CompositeProjectionComponent3Step<V1, V2> {

	final SearchProjection<V1> component1;
	final SearchProjection<V2> component2;

	public CompositeProjectionComponent3StepImpl(SearchProjectionDslContext<?> dslContext,
			SearchProjection<V1> component1, SearchProjection<V2> component2) {
		super( dslContext );
		this.component1 = component1;
		this.component2 = component2;
	}

	@Override
	public <V3> CompositeProjectionComponent4Step<V1, V2, V3> add(SearchProjection<V3> projection) {
		return new CompositeProjectionComponent4StepImpl<>( dslContext, component1, component2, projection );
	}

	@Override
	public <V> CompositeProjectionOptionsStep<?, V> transform(BiFunction<V1, V2, V> transformer) {
		return new CompositeProjectionFinalStep<>( dslContext, transformer, component1, component2 );
	}

	@Override
	SearchProjection<?>[] toProjectionArray(SearchProjection<?>... otherProjections) {
		SearchProjection<?>[] array = new SearchProjection[2 + otherProjections.length];
		array[0] = component1;
		array[1] = component2;
		System.arraycopy( otherProjections, 0, array, 2, otherProjections.length );
		return array;
	}

	@Override
	SearchProjection<?>[] toProjectionArray() {
		return new SearchProjection<?>[] { component1, component2 };
	}

}
