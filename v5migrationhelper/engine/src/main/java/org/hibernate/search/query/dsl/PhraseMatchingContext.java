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
public interface PhraseMatchingContext extends FieldCustomization<PhraseMatchingContext> {
	/**
	 * field / property the term query is executed on
	 * @param field the name of the field
	 * @return {@code this} for method chaining
	 */
	PhraseMatchingContext andField(String field);

	/**
	 * Sentence to match. It will be processed by the analyzer
	 * @param sentence the sentence to match
	 * @return the {@link PhraseTermination} for method chaining
	 */
	PhraseTermination sentence(String sentence);
}
