/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate;

import java.util.function.Consumer;

import org.hibernate.search.engine.search.SearchPredicate;

/**
 * @author Yoann Rodiere
 */
public interface BooleanJunctionPredicateContext<N> extends SearchPredicateContext<BooleanJunctionPredicateContext<N>>, ExplicitEndContext<N> {

	BooleanJunctionPredicateContext<N> must(SearchPredicate searchPredicate);

	BooleanJunctionPredicateContext<N> mustNot(SearchPredicate searchPredicate);

	BooleanJunctionPredicateContext<N> should(SearchPredicate searchPredicate);

	BooleanJunctionPredicateContext<N> filter(SearchPredicate searchPredicate);

	/*
	 * Fully fluid syntax, without lambdas but requiring .end()
	 * calls from time to time to mark the end of a nested context.
	 */

	SearchPredicateContainerContext<BooleanJunctionPredicateContext<N>> must();

	SearchPredicateContainerContext<BooleanJunctionPredicateContext<N>> mustNot();

	SearchPredicateContainerContext<BooleanJunctionPredicateContext<N>> should();

	SearchPredicateContainerContext<BooleanJunctionPredicateContext<N>> filter();

	/*
	 * Alternative syntax taking advantage of lambdas,
	 * allowing to introduce if/else statements in the query building code
	 * and removing the need to call .end() on nested clause contexts.
	 */

	default BooleanJunctionPredicateContext<N> must(Consumer<SearchPredicateContainerContext<?>> clauseContributor) {
		clauseContributor.accept( must() );
		return this;
	}

	default BooleanJunctionPredicateContext<N> mustNot(Consumer<SearchPredicateContainerContext<?>> clauseContributor) {
		clauseContributor.accept( mustNot() );
		return this;
	}

	default BooleanJunctionPredicateContext<N> should(Consumer<SearchPredicateContainerContext<?>> clauseContributor) {
		clauseContributor.accept( should() );
		return this;
	}

	default BooleanJunctionPredicateContext<N> filter(Consumer<SearchPredicateContainerContext<?>> clauseContributor) {
		clauseContributor.accept( filter() );
		return this;
	}

}
