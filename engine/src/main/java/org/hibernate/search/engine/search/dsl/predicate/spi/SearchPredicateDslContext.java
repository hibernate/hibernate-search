/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate.spi;

/**
 * Represents the current context in the search DSL,
 * i.e. the current position in the predicate tree.
 */
public interface SearchPredicateDslContext<N, B> {

	/**
	 * Add a predicate contributor at the current position in the predicate tree.
	 * <p>
	 * The contributor will be called as late as possible to retrieve its contributed builder.
	 *
	 * @param contributor The contributor to add.
	 */
	void addChild(SearchPredicateContributor<? extends B> contributor);

	/**
	 * Add a sort builder at the current position in the sort tree.
	 *
	 * @param builder The builder to add.
	 */
	default void addChild(B builder) {
		addChild( new StaticSearchPredicateContributor<>( builder ) );
	}

	/**
	 * @return The context that should be exposed to the user after the current predicate has been fully defined.
	 */
	N getNextContext();

}
