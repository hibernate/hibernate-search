package org.hibernate.search.query.dsl.v2;

/**
* @author Emmanuel Bernard
*/
public interface TermMatchingContext {
	/**
	 * text searched in the term query (the term is pre-analyzer unless ignoreAnalyzer is called)
	 */
	TermCustomization matches(String text);
}
