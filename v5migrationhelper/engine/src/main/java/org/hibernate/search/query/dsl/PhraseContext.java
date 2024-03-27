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
public interface PhraseContext extends QueryCustomization<PhraseContext> {
	/**
	 * Sets the number of other words permitted between words in query phrase.
	 * If zero, then this is an exact phrase search.  For larger values this works
	 * like a <code>WITHIN</code> or <code>NEAR</code> operator.
	 *
	 * Defaults to 0
	 * @param slop the slop value
	 * @return a {@link PhraseContext}
	 */
	PhraseContext withSlop(int slop);

	/**
	 * field / property the term query is executed on
	 * @param fieldName the name of the field
	 * @return a {@link PhraseContext}
	 */
	PhraseMatchingContext onField(String fieldName);
}
