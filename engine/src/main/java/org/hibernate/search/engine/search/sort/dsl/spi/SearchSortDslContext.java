/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.sort.dsl.spi;

import java.util.function.Function;

import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.sort.SearchSort;
import org.hibernate.search.engine.search.sort.dsl.FieldSortOptionsStep;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactory;
import org.hibernate.search.engine.search.sort.dsl.SortThenStep;
import org.hibernate.search.engine.search.sort.spi.CompositeSortBuilder;
import org.hibernate.search.engine.search.sort.spi.SearchSortIndexScope;

/**
 * Represents the current context in the search DSL,
 * including in particular the search scope, the sort builder factory
 * and the knowledge of previous sorts chained using {@link SortThenStep#then()}.
 *
 * @param <SC> The type of the backend-specific search scope.
 * @param <PDF> The type of factory used to create predicates in {@link FieldSortOptionsStep#filter(Function)}.
 */
public final class SearchSortDslContext<SC extends SearchSortIndexScope<?>, PDF extends SearchPredicateFactory> {

	public static <SC extends SearchSortIndexScope<?>, PDF extends SearchPredicateFactory> SearchSortDslContext<SC, PDF> root(
			SC scope,
			Function<SearchSortDslContext<SC, PDF>, SearchSortFactory> factoryProvider,
			PDF predicateFactory) {
		return new SearchSortDslContext<>( scope, factoryProvider, null, null, predicateFactory );
	}

	private final SC scope;
	private final Function<SearchSortDslContext<SC, PDF>, SearchSortFactory> factoryProvider;
	private final SearchSortDslContext<?, ?> parent;
	private final SearchSort sort;
	private final PDF predicateFactory;

	private SearchSort compositeSort;

	private SearchSortDslContext(SC scope,
			Function<SearchSortDslContext<SC, PDF>, SearchSortFactory> factoryProvider,
			SearchSortDslContext<?, ?> parent, SearchSort sort,
			PDF predicateFactory) {
		this.scope = scope;
		this.factoryProvider = factoryProvider;
		this.parent = parent;
		this.sort = sort;
		this.predicateFactory = predicateFactory;
	}

	/**
	 * @return The search scope.
	 */
	public SC scope() {
		return scope;
	}

	/**
	 * @return A new factory to be returned by {@link SortThenStep#then()}.
	 */
	public SearchSortFactory then() {
		return factoryProvider.apply( this );
	}

	/**
	 * @param newScope The new scope for the new DSL context.
	 * @param newPredicateFactory The new predicate factory for the new DSL context.
	 * @return A copy of this DSL context with its scope and predicate factory replaced with the given ones.
	 */
	public SearchSortDslContext<SC, PDF> rescope(SC newScope, PDF newPredicateFactory) {
		return new SearchSortDslContext<>( newScope, factoryProvider, parent, sort, newPredicateFactory );
	}

	/**
	 * Create a new context with a sort appended.
	 *
	 * @param sort The sort to add.
	 * @return A new DSL context, with the given builder appended.
	 */
	public SearchSortDslContext<SC, PDF> append(SearchSort sort) {
		return new SearchSortDslContext<>( scope, factoryProvider, this, sort, predicateFactory );
	}

	/**
	 * @return The predicate factory. Will always return the exact same instance.
	 */
	public PDF predicateFactory() {
		return predicateFactory;
	}

	/**
	 * Create a {@link SearchSort} instance
	 * matching the definition given in the previous DSL steps.
	 *
	 * @return The {@link SearchSort} instance.
	 */
	public SearchSort toSort() {
		if ( compositeSort == null ) {
			compositeSort = createCompositeSort();
		}
		return compositeSort;
	}

	private SearchSort createCompositeSort() {
		if ( parent == null ) {
			// No sort at all; just use an empty composite sort.
			return scope.sortBuilders().composite().build();
		}
		else if ( parent.sort == null ) {
			// Only one element
			return sort;
		}
		else {
			CompositeSortBuilder builder = scope.sortBuilders().composite();
			collectSorts( builder );
			return builder.build();
		}
	}

	private void collectSorts(CompositeSortBuilder builder) {
		if ( sort == null ) {
			// We've reached the root
			return;
		}

		parent.collectSorts( builder );
		builder.add( sort );
	}
}
