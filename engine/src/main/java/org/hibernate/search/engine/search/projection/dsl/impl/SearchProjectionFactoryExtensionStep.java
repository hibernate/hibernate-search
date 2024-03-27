/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.projection.dsl.impl;

import java.util.function.Function;

import org.hibernate.search.engine.common.dsl.spi.DslExtensionState;
import org.hibernate.search.engine.search.projection.dsl.ProjectionFinalStep;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactoryExtension;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactoryExtensionIfSupportedMoreStep;

public final class SearchProjectionFactoryExtensionStep<P, R, E>
		implements
		SearchProjectionFactoryExtensionIfSupportedMoreStep<P, R, E> {

	private final SearchProjectionFactory<R, E> parent;

	private final DslExtensionState<ProjectionFinalStep<P>> state = new DslExtensionState<>();

	public SearchProjectionFactoryExtensionStep(SearchProjectionFactory<R, E> parent) {
		this.parent = parent;
	}

	@Override
	public <T> SearchProjectionFactoryExtensionIfSupportedMoreStep<P, R, E> ifSupported(
			SearchProjectionFactoryExtension<T, R, E> extension,
			Function<T, ? extends ProjectionFinalStep<P>> projectionContributor) {
		state.ifSupported( extension, extension.extendOptional( parent ), projectionContributor );
		return this;
	}

	@Override
	public ProjectionFinalStep<P> orElse(
			Function<SearchProjectionFactory<R, E>, ? extends ProjectionFinalStep<P>> projectionContributor) {
		return state.orElse( parent, projectionContributor );
	}

	@Override
	public ProjectionFinalStep<P> orElseFail() {
		return state.orElseFail();
	}
}
