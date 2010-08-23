package org.hibernate.search.query.dsl;

/**
 * @author Emmanuel Bernard
 */
public interface FuzzyContext extends QueryCustomization<FuzzyContext> {
	/**
	 * field / property the term query is executed on
	 */
	TermMatchingContext onField(String field);

	/**
	 * Threshold above which two terms are considered similar enough.
	 * Value between 0 and 1 (1 excluded)
	 * Defaults to .5
	 */
	FuzzyContext withThreshold(float threshold);

	/**
	 * Size of the prefix ignored by the fuzzyness.
	 * A non zero value is recommended if the index contains a huge amount of distinct terms
	 *
	 * Defaults to 0
	 */
	FuzzyContext withPrefixLength(int prefixLength);
}
