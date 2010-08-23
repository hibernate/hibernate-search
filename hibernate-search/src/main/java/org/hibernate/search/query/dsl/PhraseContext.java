package org.hibernate.search.query.dsl;

/**
 * @author Emmanuel Bernard
 */
public interface PhraseContext extends QueryCustomization<PhraseContext> {
	/**
	 * Sets the number of other words permitted between words in query phrase.
	 * If zero, then this is an exact phrase search.  For larger values this works
	 * like a <code>WITHIN</code> or <code>NEAR</code> operator.
	 *
	 * Defaults to 0
	 */
	PhraseContext withSlop(int slop);

	/**
	 * field / property the term query is executed on
	 */
	PhraseMatchingContext onField(String fieldName);
}
