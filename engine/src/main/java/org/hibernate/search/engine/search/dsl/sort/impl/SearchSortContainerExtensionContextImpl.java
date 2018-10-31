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


final class SearchSortContainerExtensionContextImpl<B> implements SearchSortContainerExtensionContext {

	private final SearchSortContainerContext parent;
	private final SearchSortFactory<?, B> factory;
	private final SearchSortDslContext<? super B> dslContext;

	private final DslExtensionState state = new DslExtensionState();

	SearchSortContainerExtensionContextImpl(SearchSortContainerContext parent,
			SearchSortFactory<?, B> factory, SearchSortDslContext<? super B> dslContext) {
		this.parent = parent;
		this.factory = factory;
		this.dslContext = dslContext;
	}

	@Override
	public <T> SearchSortContainerExtensionContext ifSupported(
			SearchSortContainerContextExtension<T> extension, Consumer<T> sortContributor) {
		state.ifSupported( extension, extension.extendOptional( parent, factory, dslContext ), sortContributor );
		return this;
	}

	@Override
	public NonEmptySortContext orElse(Consumer<SearchSortContainerContext> sortContributor) {
		state.orElse( parent, sortContributor );
		return new NonEmptySortContextImpl( parent, dslContext );
	}

	@Override
	public NonEmptySortContext orElseFail() {
		state.orElseFail();
		return new NonEmptySortContextImpl( parent, dslContext );
	}
}
