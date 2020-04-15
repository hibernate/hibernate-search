/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.aggregation.dsl.spi;

import java.util.function.Function;

import org.hibernate.search.engine.search.aggregation.spi.SearchAggregationBuilderFactory;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactoryExtension;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilderFactory;
import org.hibernate.search.engine.search.sort.dsl.FieldSortOptionsStep;

/**
 * Represents the current context in the search DSL,
 * including in particular the aggregation builder factory.
 *
 * @param <F> The type of aggregation factory.
 * @param <PDF> The type of factory used to create predicates in {@link FieldSortOptionsStep#filter(Function)}.
 */
public interface SearchAggregationDslContext<F extends SearchAggregationBuilderFactory<?>, PDF extends SearchPredicateFactory> {

	/**
	 * @return The aggregation builder factory. Will always return the exact same instance.
	 */
	F getBuilderFactory();

	/**
	 * @return The predicate builder factory. Will always return the exact same instance.
	 */
	SearchPredicateBuilderFactory<?, ?> getPredicateBuilderFactory();

	/**
	 * @return The predicate factory. Will always return the exact same instance.
	 */
	PDF getPredicateFactory();

	/**
	 * @param extension The extension to apply to the predicate factory.
	 * @param <PDF2> The type of the new predicate factory.
	 * @return A new context, identical to {@code this} except for the predicate factory which is extended.
	 */
	<PDF2 extends SearchPredicateFactory> SearchAggregationDslContext<F, PDF2> withExtendedPredicateFactory(
			SearchPredicateFactoryExtension<PDF2> extension);
}
