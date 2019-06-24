/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate;

/**
 * The final step in an "simple query string" predicate definition, where optional parameters can be set.
 */
public interface SimpleQueryStringPredicateOptionsStep
		extends PredicateFinalStep, PredicateScoreStep<SimpleQueryStringPredicateOptionsStep> {

	/**
	 * Define the default operator as AND.
	 * <p>
	 * By default, unless the query string contains explicit operators,
	 * documents will match if <em>any</em> term mentioned in the query string is present in the document (OR operator).
	 * This will change the default behavior,
	 * making document match if <em>all</em> terms mentioned in the query string are present in the document.
	 *
	 * @return {@code this}, for method chaining.
	 */
	SimpleQueryStringPredicateOptionsStep withAndAsDefaultOperator();

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
