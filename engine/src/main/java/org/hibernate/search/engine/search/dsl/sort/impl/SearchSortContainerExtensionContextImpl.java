/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.sort.impl;

import java.util.function.Consumer;

import org.hibernate.search.engine.common.dsl.impl.DslExtensionState;
import org.hibernate.search.engine.search.dsl.sort.NonEmptySortContext;
import org.hibernate.search.engine.search.dsl.sort.SearchSortContainerContext;
import org.hibernate.search.engine.search.dsl.sort.SearchSortContainerContextExtension;
import org.hibernate.search.engine.search.dsl.sort.SearchSortContainerExtensionContext;
import org.hibernate.search.engine.search.dsl.sort.spi.NonEmptySortContextImpl;
import org.hibernate.search.engine.search.dsl.sort.spi.SearchSortDslContext;
import org.hibernate.search.engine.search.sort.spi.SearchSortFactory;


final class SearchSortContainerExtensionContextImpl<N, B> implements SearchSortContainerExtensionContext<N> {

	private final SearchSortContainerContext<N> parent;
	private final SearchSortFactory<?, B> factory;
	private final SearchSortDslContext<N, ? super B> dslContext;

	private final DslExtensionState state = new DslExtensionState();

	SearchSortContainerExtensionContextImpl(SearchSortContainerContext<N> parent,
			SearchSortFactory<?, B> factory, SearchSortDslContext<N, ? super B> dslContext) {
		this.parent = parent;
		this.factory = factory;
		this.dslContext = dslContext;
	}

	@Override
	public <T> SearchSortContainerExtensionContext<N> ifSupported(
			SearchSortContainerContextExtension<N, T> extension, Consumer<T> sortContributor) {
		state.ifSupported( extension, extension.extendOptional( parent, factory, dslContext ), sortContributor );
		return this;
	}

	@Override
	public NonEmptySortContext<N> orElse(Consumer<SearchSortContainerContext<?>> sortContributor) {
		state.orElse( parent, sortContributor );
		return new NonEmptySortContextImpl<>( parent, dslContext );
	}

	@Override
	public NonEmptySortContext<N> orElseFail() {
		state.orElseFail();
		return new NonEmptySortContextImpl<>( parent, dslContext );
	}
}
