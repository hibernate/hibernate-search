/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.projection.dsl.impl;

import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.dsl.CompositeProjectionComponent4Step;
import org.hibernate.search.engine.search.projection.dsl.CompositeProjectionOptionsStep;
import org.hibernate.search.engine.search.projection.dsl.spi.SearchProjectionDslContext;
import org.hibernate.search.util.common.function.TriFunction;

class CompositeProjectionComponent4StepImpl<V1, V2, V3>
		extends AbstractCompositeProjectionComponentsAtLeastOneAddedStep
		implements CompositeProjectionComponent4Step<V1, V2, V3> {

	final SearchProjection<V1> component1;
	final SearchProjection<V2> component2;
	final SearchProjection<V3> component3;

	public CompositeProjectionComponent4StepImpl(SearchProjectionDslContext<?> dslContext,
			SearchProjection<V1> component1, SearchProjection<V2> component2, SearchProjection<V3> component3) {
		super( dslContext );
		this.component1 = component1;
		this.component2 = component2;
		this.component3 = component3;
	}

	@Override
	public <V> CompositeProjectionOptionsStep<?, V> transform(TriFunction<V1, V2, V3, V> transformer) {
		return new CompositeProjectionFinalStep<>( dslContext, transformer, component1, component2, component3 );
	}

	@Override
	SearchProjection<?>[] toProjectionArray(SearchProjection<?>... otherProjections) {
		SearchProjection<?>[] array = new SearchProjection[3 + otherProjections.length];
		array[0] = component1;
		array[1] = component2;
		array[2] = component3;
		System.arraycopy( otherProjections, 0, array, 3, otherProjections.length );
		return array;
	}

	@Override
	SearchProjection<?>[] toProjectionArray() {
		return new SearchProjection<?>[] { component1, component2, component3 };
	}

}
