/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.dsl.spi;

import org.hibernate.search.engine.search.predicate.spi.SearchPredicateIndexScope;

/**
 * Represents the current context in the search DSL,
 * including in particular the search scope and the predicate builder factory.
 *
 * @param <SC> The type of the backend-specific search scope.
 */
public final class SearchPredicateDslContext<SC extends SearchPredicateIndexScope<?>> {

	public static <SC extends SearchPredicateIndexScope<?>> SearchPredicateDslContext<SC> root(SC scope) {
		return new SearchPredicateDslContext<>( scope );
	}

	private final SC scope;

	private SearchPredicateDslContext(SC scope) {
		this.scope = scope;
	}

	/**
	 * @return The search scope.
	 */
	public SC scope() {
		return scope;
	}

	/**
	 * @param newScope The new scope for the new DSL context.
	 * @return A copy of this DSL context with its scope and predicate factory replaced with the given ones.
	 */
	public SearchPredicateDslContext<SC> rescope(SC newScope) {
		return new SearchPredicateDslContext<>( newScope );
	}

}
