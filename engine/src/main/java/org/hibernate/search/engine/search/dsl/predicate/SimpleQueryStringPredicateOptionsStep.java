/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate;

import org.hibernate.search.engine.search.common.BooleanOperator;

/**
 * The final step in an "simple query string" predicate definition, where optional parameters can be set.
 */
public interface SimpleQueryStringPredicateOptionsStep
		extends PredicateFinalStep, PredicateScoreStep<SimpleQueryStringPredicateOptionsStep> {

	/**
	 * Define the default operator.
	 * <p>
	 * By default, unless the query string contains explicit operators,
	 * documents will match if <em>any</em> term mentioned in the query string is present in the document ({@code OR} operator).
	 * This can be used to change the default behavior to {@code AND},
	 * making document match if <em>all</em> terms mentioned in the query string are present in the document.
	 *
	 * @param operator The default operator ({@code OR} or {@code AND}).
	 * @return {@code this}, for method chaining.
	 */
	SimpleQueryStringPredicateOptionsStep defaultOperator(BooleanOperator operator);

	/**
	 * @deprecated Use {@code defaultOperator(BooleanOperator.AND)} instead.
	 * @return {@code this}, for method chaining.
	 */
	@Deprecated
	default SimpleQueryStringPredicateOptionsStep withAndAsDefaultOperator() {
		return defaultOperator( BooleanOperator.AND );
	}

	/**
	 * Define an analyzer to use at query time to interpret the value to match.
	 * <p>
	 * If this method is not called, the analyzer defined on the field will be used.
	 *
	 * @param analyzerName The name of the analyzer to use in the query for this predicate.
	 * @return {@code this}, for method chaining.
	 */
	SimpleQueryStringPredicateOptionsStep analyzer(String analyzerName);

	/**
	 * Any analyzer or normalizer defined on any field will be ignored to interpret the value to match.
	 *
	 * @return {@code this}, for method chaining.
	 */
	SimpleQueryStringPredicateOptionsStep skipAnalysis();

}
