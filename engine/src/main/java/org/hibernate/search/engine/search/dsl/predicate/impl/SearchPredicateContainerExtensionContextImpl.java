/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate.impl;

import java.util.function.Consumer;

import org.hibernate.search.engine.common.dsl.impl.DslExtensionState;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateContainerContext;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateContainerContextExtension;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateContainerExtensionContext;
import org.hibernate.search.engine.search.dsl.predicate.spi.SearchPredicateDslContext;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateFactory;


final class SearchPredicateContainerExtensionContextImpl<N, B> implements SearchPredicateContainerExtensionContext<N> {

	private final SearchPredicateContainerContext<N> parent;
	private final SearchPredicateFactory<?, B> factory;
	private final SearchPredicateDslContext<N, ? super B> dslContext;

	private final DslExtensionState state = new DslExtensionState();

	SearchPredicateContainerExtensionContextImpl(SearchPredicateContainerContext<N> parent,
			SearchPredicateFactory<?, B> factory, SearchPredicateDslContext<N, ? super B> dslContext) {
		this.parent = parent;
		this.factory = factory;
		this.dslContext = dslContext;
	}

	@Override
	public <T> SearchPredicateContainerExtensionContext<N> ifSupported(
			SearchPredicateContainerContextExtension<N, T> extension, Consumer<T> predicateContributor) {
		state.ifSupported( extension, extension.extendOptional( parent, factory, dslContext ), predicateContributor );
		return this;
	}

	@Override
	public N orElse(Consumer<SearchPredicateContainerContext<?>> predicateContributor) {
		state.orElse( parent, predicateContributor );
		return dslContext.getNextContext();
	}

	@Override
	public N orElseFail() {
		state.orElseFail();
		return dslContext.getNextContext();
	}
}
