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


final class SearchPredicateContainerExtensionContextImpl<B> implements SearchPredicateContainerExtensionContext {

	private final SearchPredicateContainerContext parent;
	private final SearchPredicateFactory<?, B> factory;
	private final SearchPredicateDslContext<? super B> dslContext;

	private final DslExtensionState state = new DslExtensionState();

	SearchPredicateContainerExtensionContextImpl(SearchPredicateContainerContext parent,
			SearchPredicateFactory<?, B> factory, SearchPredicateDslContext<? super B> dslContext) {
		this.parent = parent;
		this.factory = factory;
		this.dslContext = dslContext;
	}

	@Override
	public <T> SearchPredicateContainerExtensionContext ifSupported(
			SearchPredicateContainerContextExtension<T> extension, Consumer<T> predicateContributor) {
		state.ifSupported( extension, extension.extendOptional( parent, factory, dslContext ), predicateContributor );
		return this;
	}

	@Override
	public void orElse(Consumer<SearchPredicateContainerContext> predicateContributor) {
		state.orElse( parent, predicateContributor );
	}

	@Override
	public void orElseFail() {
		state.orElseFail();
	}
}
