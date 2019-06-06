/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.sort.impl;

import java.util.function.Function;

import org.hibernate.search.engine.common.dsl.spi.DslExtensionState;
import org.hibernate.search.engine.search.dsl.sort.NonEmptySortContext;
import org.hibernate.search.engine.search.dsl.sort.SearchSortFactoryContext;
import org.hibernate.search.engine.search.dsl.sort.SearchSortFactoryContextExtension;
import org.hibernate.search.engine.search.dsl.sort.SearchSortFactoryExtensionContext;
import org.hibernate.search.engine.search.dsl.sort.SearchSortTerminalContext;
import org.hibernate.search.engine.search.dsl.sort.spi.StaticNonEmptySortContext;
import org.hibernate.search.engine.search.dsl.sort.spi.SearchSortDslContext;


final class SearchSortFactoryExtensionContextImpl<B> implements SearchSortFactoryExtensionContext {

	private final SearchSortFactoryContext parent;
	private final SearchSortDslContext<?, B> dslContext;

	private final DslExtensionState<SearchSortTerminalContext> state = new DslExtensionState<>();

	SearchSortFactoryExtensionContextImpl(SearchSortFactoryContext parent,
			SearchSortDslContext<?, B> dslContext) {
		this.parent = parent;
		this.dslContext = dslContext;
	}

	@Override
	public <T> SearchSortFactoryExtensionContext ifSupported(
			SearchSortFactoryContextExtension<T> extension,
			Function<T, ? extends SearchSortTerminalContext> sortContributor) {
		state.ifSupported( extension, extension.extendOptional( parent, dslContext ), sortContributor );
		return this;
	}

	@Override
	public NonEmptySortContext orElse(Function<SearchSortFactoryContext, ? extends SearchSortTerminalContext> sortContributor) {
		SearchSortTerminalContext result = state.orElse( parent, sortContributor );
		return new StaticNonEmptySortContext<>( dslContext, dslContext.getFactory().toImplementation( result.toSort() ) );
	}

	@Override
	public NonEmptySortContext orElseFail() {
		SearchSortTerminalContext result = state.orElseFail();
		return new StaticNonEmptySortContext<>( dslContext, dslContext.getFactory().toImplementation( result.toSort() ) );
	}
}
