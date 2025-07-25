/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.projection.dsl.impl;

import org.hibernate.search.engine.search.projection.ProjectionCollector;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.dsl.CompositeProjectionOptionsStep;
import org.hibernate.search.engine.search.projection.dsl.CompositeProjectionValueStep;
import org.hibernate.search.engine.search.projection.spi.CompositeProjectionBuilder;
import org.hibernate.search.engine.search.spi.ResultsCompositor;

public class CompositeProjectionValueStepImpl<T>
		extends CompositeProjectionOptionsStepImpl<T, T>
		implements CompositeProjectionValueStep<CompositeProjectionOptionsStepImpl<T, T>, T> {

	public CompositeProjectionValueStepImpl(CompositeProjectionBuilder builder,
			SearchProjection<?>[] inners, ResultsCompositor<?, T> compositor) {
		super( builder, inners, compositor, ProjectionCollector.nullable() );
	}

	@Override
	public <R> CompositeProjectionOptionsStep<?, R> collector(ProjectionCollector.Provider<T, R> collector) {
		return new CompositeProjectionOptionsStepImpl<>( builder, inners, compositor, collector );
	}
}
