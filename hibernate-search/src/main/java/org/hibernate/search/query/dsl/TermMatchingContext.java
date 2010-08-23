package org.hibernate.search.query.dsl;

/**
* @author Emmanuel Bernard
*/
public interface TermMatchingContext extends FieldCustomization<TermMatchingContext> {
	/**
	 * Value searched in the field or fields.
	 * The value is passed to the field's:
	 *  - field bridge
	 *  - analyzer (unless ignoreAnalyzer is called).
	 */
	TermTermination matching(Object value);

	/**
	 * field / property the term query is executed on
	 */
	TermMatchingContext andField(String field);
}
