/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.projection.dsl.impl;

import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.dsl.CompositeProjectionOptionsStep;
import org.hibernate.search.engine.search.projection.spi.CompositeProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.ProjectionAccumulator;
import org.hibernate.search.engine.search.projection.spi.ProjectionCompositor;

public class CompositeProjectionOptionsStepImpl<T, P>
		implements CompositeProjectionOptionsStep<CompositeProjectionOptionsStepImpl<T, P>, P> {

	final CompositeProjectionBuilder builder;
	final SearchProjection<?>[] inners;
	final ProjectionCompositor<?, T> compositor;
	private final ProjectionAccumulator.Provider<T, P> accumulatorProvider;

	public CompositeProjectionOptionsStepImpl(CompositeProjectionBuilder builder,
			SearchProjection<?>[] inners, ProjectionCompositor<?, T> compositor,
			ProjectionAccumulator.Provider<T, P> accumulatorProvider) {
		this.builder = builder;
		this.inners = inners;
		this.compositor = compositor;
		this.accumulatorProvider = accumulatorProvider;
	}

	@Override
	public SearchProjection<P> toProjection() {
		return builder.build( inners, compositor, accumulatorProvider );
	}
}
