/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

package org.hibernate.search.query.dsl;

import org.apache.lucene.search.Query;

/**
 * Represents a boolean query that can contains one or more elements to join
 *
 * <h2 id="minimumshouldmatch">"minimumShouldMatch" constraints</h2>
 * <p>
 * "minimumShouldMatch" constraints define a minimum number of "should" clauses that have to match
 * in order for the boolean junction to match.
 * <p>
 * The feature is similar, and will work identically, to
 * <a href="https://lucene.apache.org/solr/7_3_0/solr-core/org/apache/solr/util/doc-files/min-should-match.html">"Min Number Should Match"</a>
 * in Solr or
 * <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl-minimum-should-match.html">{@code minimum_should_match}</a>
 * in Elasticsearch.
 * <h3 id="minimumshouldmatch-minimum">Definition of the minimum</h3>
 * <p>
 * The minimum may be defined either directly as a positive number, or indirectly as a negative number
 * or positive or negative percentage representing a ratio of the total number of "should" clauses in this boolean junction.
 * <p>
 * Here is how each type of input is interpreted:
 * <dl>
 *     <dt>Positive number</dt>
 *     <dd>
 *         The value is interpreted directly as the minimum number of "should" clauses that have to match.
 *     </dd>
 *     <dt>Negative number</dt>
 *     <dd>
 *         The absolute value is interpreted as the maximum number of "should" clauses that may not match:
 *         the absolute value is subtracted from the total number of "should" clauses.
 *     </dd>
 *     <dt>Positive percentage</dt>
 *     <dd>
 *         The value is interpreted as the minimum percentage of the total number of "should" clauses that have to match:
 *         the percentage is applied to the total number of "should" clauses, then rounded down.
 *     </dd>
 *     <dt>Negative percentage</dt>
 *     <dd>
 *         The absolute value is interpreted as the maximum percentage of the total number of "should" clauses that may not match:
 *         the absolute value of the percentage is applied to the total number of "should" clauses, then rounded down,
 *         then subtracted from the total number of "should" clauses.
 *     </dd>
 * </dl>
 * <p>
 * In any case, if the computed minimum is 0 or less, or higher than the total number of "should" clauses,
 * behavior is backend-specific (it may throw an exception, or produce unpredictable results,
 * or fall back to some default behavior).
 * <p>
 * Examples:
 * <pre><code>
 *     // Example 1: at least 3 "should" clauses have to match
 *     booleanContext1.minimumShouldMatchNumber( 3 );
 *     // Example 2: at most 2 "should" clauses may not match
 *     booleanContext2.minimumShouldMatchNumber( -2 );
 *     // Example 3: at least 75% of "should" clauses have to match (rounded down)
 *     booleanContext3.minimumShouldMatchPercent( 75 );
 *     // Example 4: at most 25% of "should" clauses may not match (rounded down)
 *     booleanContext4.minimumShouldMatchPercent( -25 );
 * </code></pre>
 *
 * @author Emmanuel Bernard
 * @deprecated See the deprecation note on {@link QueryBuilder}.
 */
@Deprecated
public interface BooleanJunction<T extends BooleanJunction> extends QueryCustomization<T>, Termination {
	/**
	 * The boolean query results should match the subquery
	 * @param query the query to match (nulls are ignored)
	 * @return a {@link BooleanJunction}
	 */
	BooleanJunction should(Query query);

	/**
	 * The boolean query results must (or must not) match the subquery
	 * Call the .not() method to ensure results of the boolean query do NOT match the subquery.
	 *
	 * @param query the query to match (nulls are ignored)
	 * @return a {@link MustJunction}
	 */
	MustJunction must(Query query);

	/**
	 * @return true if no restrictions have been applied
	 */
	boolean isEmpty();

	/**
	 * Sets the <a href="#minimumshouldmatch">"minimumShouldMatch" constraint</a>.
	 *
	 * @param matchingClausesNumber A definition of the number of "should" clauses that have to match.
	 * If positive, it is the number of clauses that have to match.
	 * See <a href="BooleanJunction.html#minimumshouldmatch-minimum">Definition of the minimum</a>
	 * for details and possible values, in particular negative values.
	 * @return {@code this}, for method chaining.
	 */
	BooleanJunction minimumShouldMatchNumber(int matchingClausesNumber);

	/**
	 * Sets the <a href="#minimumshouldmatch">"minimumShouldMatch" constraint</a>.
	 *
	 * @param matchingClausesPercent A definition of the number of "should" clauses that have to match, as a percentage.
	 * If positive, it is the percentage of the total number of "should" clauses that have to match.
	 * See <a href="BooleanJunction.html#minimumshouldmatch-minimum">Definition of the minimum</a>
	 * for details and possible values, in particular negative values.
	 * @return {@code this}, for method chaining.
	 */
	BooleanJunction minimumShouldMatchPercent(int matchingClausesPercent);
}
