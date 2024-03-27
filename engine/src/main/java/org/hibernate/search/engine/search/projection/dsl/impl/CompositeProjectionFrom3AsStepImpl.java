/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.projection.dsl.impl;

import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.dsl.CompositeProjectionFrom3AsStep;
import org.hibernate.search.engine.search.projection.dsl.CompositeProjectionValueStep;
import org.hibernate.search.engine.search.projection.spi.CompositeProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.ProjectionCompositor;
import org.hibernate.search.util.common.function.TriFunction;

class CompositeProjectionFrom3AsStepImpl<V1, V2, V3>
		extends AbstractCompositeProjectionFromAsStep
		implements CompositeProjectionFrom3AsStep<V1, V2, V3> {

	final SearchProjection<V1> inner1;
	final SearchProjection<V2> inner2;
	final SearchProjection<V3> inner3;

	public CompositeProjectionFrom3AsStepImpl(CompositeProjectionBuilder builder,
			SearchProjection<V1> inner1, SearchProjection<V2> inner2, SearchProjection<V3> inner3) {
		super( builder );
		this.inner1 = inner1;
		this.inner2 = inner2;
		this.inner3 = inner3;
	}

	@Override
	public <V> CompositeProjectionValueStep<?, V> as(TriFunction<V1, V2, V3, V> transformer) {
		return new CompositeProjectionValueStepImpl<>( builder, toProjectionArray(),
				ProjectionCompositor.from( transformer )
		);
	}

	@Override
	SearchProjection<?>[] toProjectionArray() {
		return new SearchProjection<?>[] { inner1, inner2, inner3 };
	}

}
