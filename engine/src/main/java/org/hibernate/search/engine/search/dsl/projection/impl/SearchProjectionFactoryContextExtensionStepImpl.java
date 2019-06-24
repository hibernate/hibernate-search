/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.projection.impl;

import java.util.function.Function;

import org.hibernate.search.engine.common.dsl.spi.DslExtensionState;
import org.hibernate.search.engine.search.dsl.projection.SearchProjectionFactoryContext;
import org.hibernate.search.engine.search.dsl.projection.SearchProjectionFactoryContextExtension;
import org.hibernate.search.engine.search.dsl.projection.SearchProjectionFactoryContextExtensionStep;
import org.hibernate.search.engine.search.dsl.projection.ProjectionFinalStep;
import org.hibernate.search.engine.search.projection.spi.SearchProjectionBuilderFactory;

public class SearchProjectionFactoryContextExtensionStepImpl<P, R, E> implements
		SearchProjectionFactoryContextExtensionStep<P, R, E> {

	private final SearchProjectionFactoryContext<R, E> parent;
	private final SearchProjectionBuilderFactory factory;

	private final DslExtensionState<ProjectionFinalStep<P>> state = new DslExtensionState<>();

	SearchProjectionFactoryContextExtensionStepImpl(SearchProjectionFactoryContext<R, E> parent,
			SearchProjectionBuilderFactory factory) {
		this.parent = parent;
		this.factory = factory;
	}

	@Override
	public <T> SearchProjectionFactoryContextExtensionStep<P, R, E> ifSupported(
			SearchProjectionFactoryContextExtension<T, R, E> extension,
			Function<T, ? extends ProjectionFinalStep<P>> projectionContributor) {
		state.ifSupported( extension, extension.extendOptional( parent, factory ), projectionContributor );
		return this;
	}

	@Override
	public ProjectionFinalStep<P> orElse(
			Function<SearchProjectionFactoryContext<R, E>, ? extends ProjectionFinalStep<P>> projectionContributor) {
		return state.orElse( parent, projectionContributor );
	}

	@Override
	public ProjectionFinalStep<P> orElseFail() {
		return state.orElseFail();
	}
}
