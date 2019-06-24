/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.sort.spi;

import org.hibernate.search.engine.search.SearchSort;
import org.hibernate.search.engine.search.dsl.sort.SortThenStep;
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
 */
public interface SearchSortDslContext<F extends SearchSortBuilderFactory<?, B>, B> {

	/**
	 * @return The sort builder factory. Will always return the exact same instance.
	 */
	F getBuilderFactory();

	/**
	 * Create a new context with a sort builder appended.
	 *
	 * @param builder The builder to add.
	 * @return A new DSL context, with the given builder appended.
	 */
	SearchSortDslContext<?, B> append(B builder);

	/**
	 * Create a {@link SearchSort} instance
	 * matching the definition given in the previous DSL steps.
	 *
	 * @return The {@link SearchSort} instance.
	 */
	SearchSort toSort();

}
