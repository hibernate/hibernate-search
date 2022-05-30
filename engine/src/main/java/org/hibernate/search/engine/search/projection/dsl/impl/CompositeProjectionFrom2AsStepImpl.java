/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.projection.dsl.impl;

import java.util.function.BiFunction;

import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.dsl.CompositeProjectionFrom2AsStep;
import org.hibernate.search.engine.search.projection.dsl.CompositeProjectionValueStep;
import org.hibernate.search.engine.search.projection.spi.CompositeProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.ProjectionCompositor;

class CompositeProjectionFrom2AsStepImpl<V1, V2>
		extends AbstractCompositeProjectionFromAsStep
		implements CompositeProjectionFrom2AsStep<V1, V2> {

	final SearchProjection<V1> inner1;
	final SearchProjection<V2> inner2;

	public CompositeProjectionFrom2AsStepImpl(CompositeProjectionBuilder builder,
			SearchProjection<V1> inner1, SearchProjection<V2> inner2) {
		super( builder );
		this.inner1 = inner1;
		this.inner2 = inner2;
	}

	@Override
	public <V> CompositeProjectionValueStep<?, V> as(BiFunction<V1, V2, V> transformer) {
		return new CompositeProjectionValueStepImpl<>( builder, toProjectionArray(),
				ProjectionCompositor.from( transformer )
		);
	}

	@Override
	SearchProjection<?>[] toProjectionArray() {
		return new SearchProjection<?>[] { inner1, inner2 };
	}

}
