/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate.impl;

import java.util.function.Function;

import org.hibernate.search.engine.common.dsl.impl.DslExtensionState;
import org.hibernate.search.engine.search.SearchPredicate;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactoryContext;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactoryContextExtension;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactoryExtensionContext;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilderFactory;


final class SearchPredicateFactoryExtensionContextImpl<B> implements SearchPredicateFactoryExtensionContext {

	private final SearchPredicateFactoryContext parent;
	private final SearchPredicateBuilderFactory<?, B> factory;

	private final DslExtensionState<SearchPredicate> state = new DslExtensionState<>();

	SearchPredicateFactoryExtensionContextImpl(SearchPredicateFactoryContext parent,
			SearchPredicateBuilderFactory<?, B> factory) {
		this.parent = parent;
		this.factory = factory;
	}

	@Override
	public <T> SearchPredicateFactoryExtensionContext ifSupported(
			SearchPredicateFactoryContextExtension<T> extension, Function<T, SearchPredicate> predicateContributor) {
		state.ifSupported( extension, extension.extendOptional( parent, factory ), predicateContributor );
		return this;
	}

	@Override
	public SearchPredicate orElse(Function<SearchPredicateFactoryContext, SearchPredicate> predicateContributor) {
		return state.orElse( parent, predicateContributor );
	}

	@Override
	public SearchPredicate orElseFail() {
		return state.orElseFail();
	}
}
