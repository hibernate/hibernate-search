package org.hibernate.search.query.dsl.v2;

/**
 * @author Emmanuel Bernard
 */
public interface FieldCustomization<T> {
	/**
	 * Boost the field to a given value
	 * Most of the time positive float:
	 *  - lower than 1 to diminish the weight
	 *  - higher than 1 to increase the weight
	 *
	 * Could be negative but not unless you understand what is going on (advanced)
	 */
	T boostedTo(float boost);

	/**
	 * Advanced
	 * Do not execute the analyzer on the text.
	 * (It is usually a good idea to apply the analyzer)
	 */
	T ignoreAnalyzer();
}
