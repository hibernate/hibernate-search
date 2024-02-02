/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.dsl;

import java.util.function.Consumer;

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
	 * TODO: WILL BE MOVED.
	 * @param matchingClausesNumber
	 * @return {@code this}, for method chaining.
	 */
	default S minimumShouldMatchNumber(int matchingClausesNumber) {
		return minimumShouldMatch()
				.ifMoreThan( 0 ).thenRequireNumber( matchingClausesNumber )
				.end();
	}

	/**
	 * TODO: WILL BE MOVED.
	 * @param matchingClausesPercent
	 * @return {@code this}, for method chaining.
	 */
	default S minimumShouldMatchPercent(int matchingClausesPercent) {
		return minimumShouldMatch()
				.ifMoreThan( 0 ).thenRequirePercent( matchingClausesPercent )
				.end();
	}

	MinimumShouldMatchConditionStep<? extends S> minimumShouldMatch();

	/**
	 * TODO: WILL BE MOVED.
	 * @param constraintContributor
	 * @return {@code this}, for method chaining.
	 */
	S minimumShouldMatch(
			Consumer<? super MinimumShouldMatchConditionStep<?>> constraintContributor);

	/**
	 * Sets the slop, which defines how permissive the phrase predicate will be.
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
