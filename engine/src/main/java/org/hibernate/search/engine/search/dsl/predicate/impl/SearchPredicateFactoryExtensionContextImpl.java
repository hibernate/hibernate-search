/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate.impl;

import java.util.function.Function;

import org.hibernate.search.engine.common.dsl.spi.DslExtensionState;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactoryContext;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactoryContextExtension;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactoryExtensionContext;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateTerminalContext;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilderFactory;


final class SearchPredicateFactoryExtensionContextImpl<B> implements SearchPredicateFactoryExtensionContext {

	private final SearchPredicateFactoryContext parent;
	private final SearchPredicateBuilderFactory<?, B> factory;

	private final DslExtensionState<SearchPredicateTerminalContext> state = new DslExtensionState<>();

	SearchPredicateFactoryExtensionContextImpl(SearchPredicateFactoryContext parent,
			SearchPredicateBuilderFactory<?, B> factory) {
		this.parent = parent;
		this.factory = factory;
	}

	@Override
	public <T> SearchPredicateFactoryExtensionContext ifSupported(
			SearchPredicateFactoryContextExtension<T> extension,
			Function<T, ? extends SearchPredicateTerminalContext> predicateContributor) {
		state.ifSupported( extension, extension.extendOptional( parent, factory ), predicateContributor );
		return this;
	}

	@Override
	public SearchPredicateTerminalContext orElse(
			Function<SearchPredicateFactoryContext, ? extends SearchPredicateTerminalContext> predicateContributor) {
		return state.orElse( parent, predicateContributor );
	}

	@Override
	public SearchPredicateTerminalContext orElseFail() {
		return state.orElseFail();
	}
}
