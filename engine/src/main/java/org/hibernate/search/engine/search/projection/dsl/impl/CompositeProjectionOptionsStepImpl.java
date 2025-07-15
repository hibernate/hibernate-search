/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.projection.dsl.impl;

import org.hibernate.search.engine.search.projection.ProjectionCollector;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.dsl.CompositeProjectionOptionsStep;
import org.hibernate.search.engine.search.projection.spi.CompositeProjectionBuilder;
import org.hibernate.search.engine.search.spi.ResultsCompositor;

public class CompositeProjectionOptionsStepImpl<T, P>
		implements CompositeProjectionOptionsStep<CompositeProjectionOptionsStepImpl<T, P>, P> {

	final CompositeProjectionBuilder builder;
	final SearchProjection<?>[] inners;
	final ResultsCompositor<?, T> compositor;
	private final ProjectionCollector.Provider<T, P> collectorProvider;

	public CompositeProjectionOptionsStepImpl(CompositeProjectionBuilder builder,
			SearchProjection<?>[] inners, ResultsCompositor<?, T> compositor,
			ProjectionCollector.Provider<T, P> collectorProvider) {
		this.builder = builder;
		this.inners = inners;
		this.compositor = compositor;
		this.collectorProvider = collectorProvider;
	}

	@Override
	public SearchProjection<P> toProjection() {
		return builder.build( inners, compositor, collectorProvider );
	}
}
