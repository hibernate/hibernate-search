package org.hibernate.search.query.dsl.v2;

/**
* @author Emmanuel Bernard
*/
public interface TermMatchingContext extends FieldCustomization<TermMatchingContext> {
	/**
	 * text searched in the term query (the term is pre-analyzer unless ignoreAnalyzer is called)
	 */
	TermTermination matches(String text);

//	/**
//	 * field / property the term query is executed on
//	 */
//	TermMatchingContext onField(String field);
}
