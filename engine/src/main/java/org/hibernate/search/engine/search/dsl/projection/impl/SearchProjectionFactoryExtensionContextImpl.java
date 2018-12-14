/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.projection.impl;

import java.util.function.Function;

import org.hibernate.search.engine.common.dsl.spi.DslExtensionState;
import org.hibernate.search.engine.search.SearchProjection;
import org.hibernate.search.engine.search.dsl.projection.SearchProjectionFactoryContext;
import org.hibernate.search.engine.search.dsl.projection.SearchProjectionFactoryContextExtension;
import org.hibernate.search.engine.search.dsl.projection.SearchProjectionFactoryExtensionContext;
import org.hibernate.search.engine.search.projection.spi.SearchProjectionBuilderFactory;

public class SearchProjectionFactoryExtensionContextImpl<P, R, O> implements SearchProjectionFactoryExtensionContext<P, R, O> {

	private final SearchProjectionFactoryContext<R, O> parent;
	private final SearchProjectionBuilderFactory factory;

	private final DslExtensionState<SearchProjection<P>> state = new DslExtensionState<>();

	SearchProjectionFactoryExtensionContextImpl(SearchProjectionFactoryContext<R, O> parent,
			SearchProjectionBuilderFactory factory) {
		this.parent = parent;
		this.factory = factory;
	}

	@Override
	public <T> SearchProjectionFactoryExtensionContext<P, R, O> ifSupported(
			SearchProjectionFactoryContextExtension<T, R, O> extension, Function<T, SearchProjection<P>> projectionContributor) {
		state.ifSupported( extension, extension.extendOptional( parent, factory ), projectionContributor );
		return this;
	}

	@Override
	public SearchProjection<P> orElse(Function<SearchProjectionFactoryContext<R, O>, SearchProjection<P>> projectionContributor) {
		return state.orElse( parent, projectionContributor );
	}

	@Override
	public SearchProjection<P> orElseFail() {
		return state.orElseFail();
	}
}
