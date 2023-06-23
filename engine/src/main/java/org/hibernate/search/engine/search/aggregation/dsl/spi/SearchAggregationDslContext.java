/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.aggregation.dsl.spi;

import java.util.function.Function;

import org.hibernate.search.engine.search.aggregation.spi.SearchAggregationIndexScope;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.sort.dsl.FieldSortOptionsStep;

/**
 * Represents the current context in the search DSL,
 * including in particular the search scope and the aggregation builder factory.
 *
 * @param <SC> The type of the backend-specific search scope.
 * @param <PDF> The type of factory used to create predicates in {@link FieldSortOptionsStep#filter(Function)}.
 */
public class SearchAggregationDslContext<SC extends SearchAggregationIndexScope<?>, PDF extends SearchPredicateFactory> {
	public static <
			SC extends SearchAggregationIndexScope<?>,
			PDF extends SearchPredicateFactory> SearchAggregationDslContext<SC, PDF> root(SC scope, PDF predicateFactory) {
		return new SearchAggregationDslContext<>( scope, predicateFactory );
	}

	private final SC scope;
	private final PDF predicateFactory;

	private SearchAggregationDslContext(SC scope, PDF predicateFactory) {
		this.scope = scope;
		this.predicateFactory = predicateFactory;
	}

	/**
	 * @return The search scope.
	 */
	public SC scope() {
		return scope;
	}

	/**
	 * @param newScope The new scope for the new DSL context.
	 * @param newPredicateFactory The new predicate factory for the new DSL context.
	 * @return A copy of this DSL context with its scope and predicate factory replaced with the given ones.
	 */
	public SearchAggregationDslContext<SC, PDF> rescope(SC newScope, PDF newPredicateFactory) {
		return new SearchAggregationDslContext<>( newScope, newPredicateFactory );
	}

	/**
	 * @return The predicate factory. Will always return the exact same instance.
	 */
	public PDF predicateFactory() {
		return predicateFactory;
	}

}
