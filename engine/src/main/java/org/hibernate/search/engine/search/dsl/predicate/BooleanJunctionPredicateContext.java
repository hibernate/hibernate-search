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

	SearchPredicateContainerContext<? extends BooleanJunctionPredicateContext<N>> must();

	SearchPredicateContainerContext<? extends BooleanJunctionPredicateContext<N>> mustNot();

	SearchPredicateContainerContext<? extends BooleanJunctionPredicateContext<N>> should();

	SearchPredicateContainerContext<? extends BooleanJunctionPredicateContext<N>> filter();

	/*
	 * Alternative syntax taking advantage of lambdas,
	 * allowing to introduce if/else statements in the query building code
	 * and removing the need to call .end() on nested clause contexts.
	 */

	BooleanJunctionPredicateContext<N> must(Consumer<? super SearchPredicateContainerContext<?>> clauseContributor);

	BooleanJunctionPredicateContext<N> mustNot(Consumer<? super SearchPredicateContainerContext<?>> clauseContributor);

	BooleanJunctionPredicateContext<N> should(Consumer<? super SearchPredicateContainerContext<?>> clauseContributor);

	BooleanJunctionPredicateContext<N> filter(Consumer<? super SearchPredicateContainerContext<?>> clauseContributor);

}
