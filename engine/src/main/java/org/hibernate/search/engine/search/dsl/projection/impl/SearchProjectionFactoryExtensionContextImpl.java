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
import org.hibernate.search.engine.search.dsl.projection.SearchProjectionFactoryExtensionContext;
import org.hibernate.search.engine.search.dsl.projection.SearchProjectionTerminalContext;
import org.hibernate.search.engine.search.projection.spi.SearchProjectionBuilderFactory;

public class SearchProjectionFactoryExtensionContextImpl<P, R, E> implements SearchProjectionFactoryExtensionContext<P, R, E> {

	private final SearchProjectionFactoryContext<R, E> parent;
	private final SearchProjectionBuilderFactory factory;

	private final DslExtensionState<SearchProjectionTerminalContext<P>> state = new DslExtensionState<>();

	SearchProjectionFactoryExtensionContextImpl(SearchProjectionFactoryContext<R, E> parent,
			SearchProjectionBuilderFactory factory) {
		this.parent = parent;
		this.factory = factory;
	}

	@Override
	public <T> SearchProjectionFactoryExtensionContext<P, R, E> ifSupported(
			SearchProjectionFactoryContextExtension<T, R, E> extension,
			Function<T, ? extends SearchProjectionTerminalContext<P>> projectionContributor) {
		state.ifSupported( extension, extension.extendOptional( parent, factory ), projectionContributor );
		return this;
	}

	@Override
	public SearchProjectionTerminalContext<P> orElse(
			Function<SearchProjectionFactoryContext<R, E>, ? extends SearchProjectionTerminalContext<P>> projectionContributor) {
		return state.orElse( parent, projectionContributor );
	}

	@Override
	public SearchProjectionTerminalContext<P> orElseFail() {
		return state.orElseFail();
	}
}
