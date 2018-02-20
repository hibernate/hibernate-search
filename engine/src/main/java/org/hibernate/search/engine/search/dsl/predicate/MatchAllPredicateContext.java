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
 * The context used when starting to define a match all predicate.
 *
 * @param <N> The type of the next context (returned by {@link ExplicitEndContext#end()}).
 */
public interface MatchAllPredicateContext<N> extends SearchPredicateContext<MatchAllPredicateContext<N>>, ExplicitEndContext<N> {

	MatchAllPredicateContext<N> except(SearchPredicate searchPredicate);

	/*
	 * Fully fluid syntax.
	 */

	SearchPredicateContainerContext<? extends MatchAllPredicateContext<N>> except();

	/*
	 * Alternative syntax taking advantage of lambdas,
	 * allowing to introduce if/else statements in the query building code.
	 */

	MatchAllPredicateContext<N> except(Consumer<? super SearchPredicateContainerContext<?>> clauseContributor);

}
