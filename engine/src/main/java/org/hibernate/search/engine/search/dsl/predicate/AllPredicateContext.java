/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate;

import java.util.function.Consumer;

import org.hibernate.search.engine.search.SearchPredicate;
import org.hibernate.search.engine.search.dsl.ExplicitEndContext;

/**
 * The context used when defining a boolean junction, allowing in particular to add clauses.
 *
 * @param <N> The type of the next context (returned by {@link ExplicitEndContext#end()}).
 */
public interface AllPredicateContext<N> extends SearchPredicateContext<AllPredicateContext<N>>, ExplicitEndContext<N> {

	AllPredicateContext<N> except(SearchPredicate searchPredicate);

	/*
	 * Fully fluid syntax, without lambdas but requiring .end()
	 * calls from time to time to mark the end of a nested context.
	 */

	SearchPredicateContainerContext<? extends AllPredicateContext<N>> except();

	/*
	 * Alternative syntax taking advantage of lambdas,
	 * allowing to introduce if/else statements in the query building code
	 * and removing the need to call .end() on nested clause contexts.
	 */

	AllPredicateContext<N> except(Consumer<? super SearchPredicateContainerContext<?>> clauseContributor);

}
