/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.projection.dsl.impl;

import java.util.List;

import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.dsl.CompositeProjectionOptionsStep;
import org.hibernate.search.engine.search.projection.dsl.CompositeProjectionValueStep;
import org.hibernate.search.engine.search.projection.spi.CompositeProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.ProjectionAccumulator;
import org.hibernate.search.engine.search.projection.spi.ProjectionCompositor;

public class CompositeProjectionValueStepImpl<T>
		extends CompositeProjectionOptionsStepImpl<T, T>
		implements CompositeProjectionValueStep<CompositeProjectionOptionsStepImpl<T, T>, T> {

	public CompositeProjectionValueStepImpl(CompositeProjectionBuilder builder,
			SearchProjection<?>[] inners, ProjectionCompositor<?, T> compositor) {
		super( builder, inners, compositor, ProjectionAccumulator.single() );
	}

	@Override
	public CompositeProjectionOptionsStep<?, List<T>> multi() {
		return new CompositeProjectionOptionsStepImpl<>( builder, inners, compositor,
				ProjectionAccumulator.list() );
	}
}
