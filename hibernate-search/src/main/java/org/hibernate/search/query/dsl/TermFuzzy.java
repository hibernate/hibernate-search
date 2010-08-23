package org.hibernate.search.query.dsl;

/**
* @author Emmanuel Bernard
*/
public interface TermFuzzy extends TermTermination {
	/**
	 * Threshold above which two terms are considered similar enough.
	 * Value between 0 and 1 (1 excluded)
	 * Defaults to .5
	 */
	TermFuzzy withThreshold(float threshold);

	/**
	 * Size of the prefix ignored by the fuzzyness.
	 * A non zero value is recommended if the index contains a huge amount of distinct terms
	 *
	 * Defaults to 0
	 */
	TermFuzzy withPrefixLength(int prefixLength);
}
