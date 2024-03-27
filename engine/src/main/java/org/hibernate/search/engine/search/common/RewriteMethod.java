/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.common;

import org.hibernate.search.engine.search.predicate.dsl.QueryStringPredicateOptionsStep;

/**
 * The rewrite parameter determines:
 *
 * <ul>
 *     <li>How Lucene calculates the relevance scores for each matching document</li>
 *     <li>Whether Lucene changes the original query to a bool query or bit set</li>
 *     <li>If changed to a bool query, which term query clauses are included</li>
 * </ul>
 * <p>
 * For more details on rewrite methods, and in particular which options are allowed, see the backend specific documentation, e.g.
 * Elasticsearch <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl-multi-term-rewrite.html">rewrite parameters page</a>,
 * OpenSearch <a href="https://opensearch.org/docs/latest/query-dsl/full-text/query-string/#parameters">query string parameters table</a>
 * or Lucene <a href="https://lucene.apache.org/core/9_9_2/core/org/apache/lucene/search/MultiTermQuery.html">MultiTermQuery</a>.
 */
public enum RewriteMethod {

	/**
	 * A rewrite method that first creates a private Filter, by visiting each term in sequence and
	 * marking all docs for that term. Matching documents are assigned a constant score equal to the
	 * query's boost.
	 * <p>
	 * This method is faster than the BooleanQuery rewrite methods when the number of matched terms
	 * or matched documents is non-trivial. Also, it will never hit a {@code TooManyClauses} exception.
	 * <p>
	 * Can only be used with {@link QueryStringPredicateOptionsStep#rewriteMethod(RewriteMethod)}.
	 */
	CONSTANT_SCORE,
	/**
	 * Like {@link #SCORING_BOOLEAN} except scores are not computed. Instead, each matching
	 * document receives a constant score equal to the query's boost.
	 * <p>
	 * This rewrite method can hit {@code TooManyClauses} exception.
	 * <p>
	 * Can only be used with {@link QueryStringPredicateOptionsStep#rewriteMethod(RewriteMethod)}.
	 */
	CONSTANT_SCORE_BOOLEAN,
	/**
	 * A rewrite method that first translates each term into a should clause
	 * of a boolean query, and keeps the scores as computed by the query. Note that typically such
	 * scores are meaningless to the user, and require non-trivial CPU to compute, so it's almost
	 * always better to use {@link #CONSTANT_SCORE} instead.
	 * <p>
	 * This rewrite method can hit {@code TooManyClauses} exception.
	 * <p>
	 * Can only be used with {@link QueryStringPredicateOptionsStep#rewriteMethod(RewriteMethod)}.
	 */
	SCORING_BOOLEAN,
	/**
	 * A rewrite method that first translates each term into a should clause
	 * of a boolean query, but adjusts the frequencies used for scoring to be blended across the terms,
	 * otherwise the rarest term typically ranks highest (often not useful e.g. in the set of expanded
	 * terms in a fuzzy query).
	 * <p>
	 * This rewrite method only uses the top scoring terms, so it will not overflow the boolean max
	 * clause count.
	 * <p>
	 * Can only be used with {@link QueryStringPredicateOptionsStep#rewriteMethod(RewriteMethod, int)}.
	 */
	TOP_TERMS_BLENDED_FREQS_N,
	/**
	 * A rewrite method that first translates each term into a should clause
	 * in a boolean query, but the scores are only computed as the boost.
	 * <p>
	 * This rewrite method only uses the top scoring terms, so it will not overflow the boolean max
	 * clause count.
	 * <p>
	 * Can only be used with {@link QueryStringPredicateOptionsStep#rewriteMethod(RewriteMethod, int)}.
	 */
	TOP_TERMS_BOOST_N,
	/**
	 * A rewrite method that first translates each term into a should clause
	 * in a boolean query, and keeps the scores as computed by the query.
	 * <p>
	 * This rewrite method only uses the top scoring terms, so it will not overflow the boolean max
	 * clause count.
	 * <p>
	 * Can only be used with {@link QueryStringPredicateOptionsStep#rewriteMethod(RewriteMethod, int)}.
	 */
	TOP_TERMS_N;

}
