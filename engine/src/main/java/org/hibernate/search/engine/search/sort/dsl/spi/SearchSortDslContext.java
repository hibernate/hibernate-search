/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.sort.dsl.spi;

import java.util.function.Function;

import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactoryExtension;
import org.hibernate.search.engine.search.sort.SearchSort;
import org.hibernate.search.engine.search.sort.dsl.FieldSortOptionsStep;
import org.hibernate.search.engine.search.sort.dsl.SortThenStep;
import org.hibernate.search.engine.search.sort.spi.SearchSortBuilder;
import org.hibernate.search.engine.search.sort.spi.SearchSortBuilderFactory;

/**
 * Represents the current context in the search DSL,
 * including in particular the sort builder factory
 * and the knowledge of previous sorts chained using {@link SortThenStep#then()}.
 *
 * @param <F> The type of sort factory.
 * @param <B> The implementation type of builders.
 * This type is backend-specific. See {@link SearchSortBuilder#toImplementation()}.
 * @param <PDF> The type of factory used to create predicates in {@link FieldSortOptionsStep#filter(Function)}.
 */
public interface SearchSortDslContext<F extends SearchSortBuilderFactory<?, B>, B, PDF extends SearchPredicateFactory> {

	/**
	 * @return The sort builder factory. Will always return the exact same instance.
	 */
	F builderFactory();

	/**
	 * Create a new context with a sort builder appended.
	 *
	 * @param builder The builder to add.
	 * @return A new DSL context, with the given builder appended.
	 */
	SearchSortDslContext<?, B, PDF> append(B builder);

	/**
	 * @return The predicate factory. Will always return the exact same instance.
	 */
	PDF predicateFactory();

	/**
	 * @param extension The extension to apply to the predicate factory.
	 * @param <PDF2> The type of the new predicate factory.
	 * @return A new context, identical to {@code this} except for the predicate factory which is extended.
	 */
	<PDF2 extends SearchPredicateFactory> SearchSortDslContext<F, B, PDF2> withExtendedPredicateFactory(
			SearchPredicateFactoryExtension<PDF2> extension);

	/**
	 * Create a {@link SearchSort} instance
	 * matching the definition given in the previous DSL steps.
	 *
	 * @return The {@link SearchSort} instance.
	 */
	SearchSort toSort();

}
