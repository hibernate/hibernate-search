/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.dsl;

import org.hibernate.search.engine.search.common.RewriteMethod;

/**
 * The final step in a "query string" predicate definition, where optional parameters can be set.
 *
 * @param <S> The "self" type (the actual exposed type of this step).
 */
public interface QueryStringPredicateOptionsStep<S extends QueryStringPredicateOptionsStep<?>>
		extends CommonQueryStringPredicateOptionsStep<S> {

	/**
	 * Specifies whether {@code *} and {@code ?} are allowed as first characters of a search term.
	 * <p>
	 * Default is {@code true}.
	 *
	 * @param allowLeadingWildcard Whether wildcard characters are allowed at the start of the query string.
	 * @return {@code this}, for method chaining.
	 */
	S allowLeadingWildcard(boolean allowLeadingWildcard);

	/**
	 * When set to {@code true}, resulting queries are aware of position increments.
	 * <p>
	 * This setting is useful when the removal of stop words leaves an unwanted “gap” between terms.
	 * <p>
	 * Default is {@code true}.
	 *
	 * @param enablePositionIncrements Whether position increments are enabled.
	 * @return {@code this}, for method chaining.
	 */
	S enablePositionIncrements(boolean enablePositionIncrements);

	/**
	 * Sets the slop, which defines how permissive the phrase predicate (created when parsing the query string) will be.
	 * <p>
	 * If zero (the default), then the predicate will only match the exact phrase given (after analysis).
	 * Higher values are increasingly permissive, allowing unexpected words or words that switched position.
	 * <p>
	 * The slop represents the number of edit operations that can be applied to the phrase to match,
	 * where each edit operation moves one word by one position.
	 * See the details on the {@link PhrasePredicateOptionsStep#slop(int) phrase predicate slop} parameter,
	 * for more information about it.
	 * <p>
	 * Default is {@code 0}.
	 *
	 * @param phraseSlop The slop value.
	 * @return {@code this}, for method chaining.
	 */
	S phraseSlop(Integer phraseSlop);

	/**
	 * Determines how backend's query parser rewrites and scores multi-term queries.
	 * <p>
	 * Default is {@link RewriteMethod#CONSTANT_SCORE}.
	 *
	 * @param rewriteMethod The rewrite method to apply.
	 * @return {@code this}, for method chaining.
	 *
	 * @see RewriteMethod
	 */
	S rewriteMethod(RewriteMethod rewriteMethod);

	/**
	 * Determines how backend's query parser rewrites and scores multi-term queries.
	 * <p>
	 * Default is {@link RewriteMethod#CONSTANT_SCORE}.
	 *
	 * @param rewriteMethod The rewrite method to apply.
	 * @param n The parameter required by a rewrite method.
	 * @return {@code this}, for method chaining.
	 *
	 * @see RewriteMethod
	 */
	S rewriteMethod(RewriteMethod rewriteMethod, int n);

}
