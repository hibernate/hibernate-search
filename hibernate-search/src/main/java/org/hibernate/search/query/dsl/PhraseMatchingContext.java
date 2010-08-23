package org.hibernate.search.query.dsl;

/**
 * @author Emmanuel Bernard
 */
public interface PhraseMatchingContext extends FieldCustomization<PhraseMatchingContext> {
	/**
	 * field / property the term query is executed on
	 */
	PhraseMatchingContext andField(String field);

	/**
	 * Sentence to match. It will be processed by the analyzer
	 */
	PhraseTermination sentence(String sentence);
}
