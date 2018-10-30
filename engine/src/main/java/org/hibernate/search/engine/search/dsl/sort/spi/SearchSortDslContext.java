/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.sort.spi;

import org.hibernate.search.engine.search.SearchSort;

/**
 * Represents the current context in the search DSL,
 * i.e. the current position in the sort tree.
 */
public interface SearchSortDslContext<B> {

	/**
	 * Add a sort contributor at the current position in the sort tree.
	 * <p>
	 * The contributor will be called as late as possible to retrieve its contributed builders.
	 *
	 * @param contributor The contributor to add.
	 */
	void addChild(SearchSortContributor<? extends B> contributor);

	/**
	 * Add a sort builder at the current position in the sort tree.
	 *
	 * @param builder The builder to add.
	 */
	default void addChild(B builder) {
		addChild( new StaticSearchSortContributor<>( builder ) );
	}

	/**
	 * Create a {@link SearchSort} instance
	 * matching the definition given in the previous DSL steps.
	 *
	 * @return The {@link SearchSort} instance.
	 */
	SearchSort toSort();

}
