/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

package org.hibernate.search.query.dsl;

/**
* @author Emmanuel Bernard
 * @deprecated See the deprecation note on {@link QueryBuilder}.
*/
@Deprecated
public interface TermFuzzy extends TermTermination {
	/**
	 * Threshold above which two terms are considered similar enough.
	 * Value between 0 and 1 (1 excluded)
	 * Defaults to .5
	 * @param threshold the value for the threshold
	 * @return a {@link TermFuzzy}
	 */
	TermFuzzy withThreshold(float threshold);

	/**
	 * Size of the prefix ignored by the fuzzyness.
	 * A non zero value is recommended if the index contains a huge amount of distinct terms
	 *
	 * Defaults to 0
	 * @param prefixLength the length of the prefix
	 * @return a {@link TermFuzzy}
	 */
	TermFuzzy withPrefixLength(int prefixLength);
}
