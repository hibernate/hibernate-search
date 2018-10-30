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
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateContainerContext;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateContainerContextExtension;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateContainerExtensionContext;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateFactory;


final class SearchPredicateContainerExtensionContextImpl<B> implements SearchPredicateContainerExtensionContext {

	private final SearchPredicateContainerContext parent;
	private final SearchPredicateFactory<?, B> factory;

	private final DslExtensionState<SearchPredicate> state = new DslExtensionState<>();

	SearchPredicateContainerExtensionContextImpl(SearchPredicateContainerContext parent,
			SearchPredicateFactory<?, B> factory) {
		this.parent = parent;
		this.factory = factory;
	}

	@Override
	public <T> SearchPredicateContainerExtensionContext ifSupported(
			SearchPredicateContainerContextExtension<T> extension, Function<T, SearchPredicate> predicateContributor) {
		state.ifSupported( extension, extension.extendOptional( parent, factory ), predicateContributor );
		return this;
	}

	@Override
	public SearchPredicate orElse(Function<SearchPredicateContainerContext, SearchPredicate> predicateContributor) {
		return state.orElse( parent, predicateContributor );
	}

	@Override
	public SearchPredicate orElseFail() {
		return state.orElseFail();
	}
}
