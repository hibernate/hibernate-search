/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.projection.dsl.impl;

import java.util.List;
import java.util.function.Function;

import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.dsl.CompositeProjectionFromAsStep;
import org.hibernate.search.engine.search.projection.dsl.CompositeProjectionValueStep;
import org.hibernate.search.engine.search.projection.spi.CompositeProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.ProjectionCompositor;

abstract class AbstractCompositeProjectionFromAsStep
		implements CompositeProjectionFromAsStep {

	final CompositeProjectionBuilder builder;

	public AbstractCompositeProjectionFromAsStep(CompositeProjectionBuilder builder) {
		this.builder = builder;
	}

	@Override
	public final CompositeProjectionValueStep<?, List<?>> asList() {
		SearchProjection<?>[] inners = toProjectionArray();
		return new CompositeProjectionValueStepImpl<>( builder, inners,
				ProjectionCompositor.fromList( inners.length ) );
	}

	@Override
	public final <V> CompositeProjectionValueStep<?, V> asList(Function<? super List<?>, ? extends V> transformer) {
		SearchProjection<?>[] inners = toProjectionArray();
		return new CompositeProjectionValueStepImpl<>( builder, inners,
				ProjectionCompositor.fromList( inners.length, transformer ) );
	}

	@Override
	public CompositeProjectionValueStep<?, Object[]> asArray() {
		SearchProjection<?>[] inners = toProjectionArray();
		return new CompositeProjectionValueStepImpl<>( builder, inners,
				ProjectionCompositor.fromArray( inners.length ) );
	}

	@Override
	public <V> CompositeProjectionValueStep<?, V> asArray(Function<? super Object[], ? extends V> transformer) {
		SearchProjection<?>[] inners = toProjectionArray();
		return new CompositeProjectionValueStepImpl<>( builder, inners,
				ProjectionCompositor.fromArray( inners.length, transformer ) );
	}

	abstract SearchProjection<?>[] toProjectionArray();

}
