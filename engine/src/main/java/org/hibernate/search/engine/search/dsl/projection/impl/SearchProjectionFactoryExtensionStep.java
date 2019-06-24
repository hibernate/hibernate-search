/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.projection.impl;

import java.util.function.Function;

import org.hibernate.search.engine.common.dsl.spi.DslExtensionState;
import org.hibernate.search.engine.search.dsl.projection.SearchProjectionFactory;
import org.hibernate.search.engine.search.dsl.projection.SearchProjectionFactoryExtension;
import org.hibernate.search.engine.search.dsl.projection.SearchProjectionFactoryExtensionIfSupportedMoreStep;
import org.hibernate.search.engine.search.dsl.projection.ProjectionFinalStep;
import org.hibernate.search.engine.search.projection.spi.SearchProjectionBuilderFactory;

public class SearchProjectionFactoryExtensionStep<P, R, E> implements
		SearchProjectionFactoryExtensionIfSupportedMoreStep<P, R, E> {

	private final SearchProjectionFactory<R, E> parent;
	private final SearchProjectionBuilderFactory factory;

	private final DslExtensionState<ProjectionFinalStep<P>> state = new DslExtensionState<>();

	SearchProjectionFactoryExtensionStep(SearchProjectionFactory<R, E> parent,
			SearchProjectionBuilderFactory factory) {
		this.parent = parent;
		this.factory = factory;
	}

	@Override
	public <T> SearchProjectionFactoryExtensionIfSupportedMoreStep<P, R, E> ifSupported(
			SearchProjectionFactoryExtension<T, R, E> extension,
			Function<T, ? extends ProjectionFinalStep<P>> projectionContributor) {
		state.ifSupported( extension, extension.extendOptional( parent, factory ), projectionContributor );
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
