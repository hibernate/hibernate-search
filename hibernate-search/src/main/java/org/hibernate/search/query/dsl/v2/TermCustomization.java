package org.hibernate.search.query.dsl.v2;

import org.apache.lucene.search.Query;

/**
* @author Emmanuel Bernard
*/
public interface TermCustomization {
	/**
	 * Advanced
	 * Do not execute the analyzer on the text.
	 * (It is usually a good idea to apply the analyzer)
	 */
	TermCustomization ignoreAnalyzer();

	/**
	 * Use a fuzzy search approximation (aka edit distance)
	 */
	TermFuzzy fuzzy();

	/**
	 * Treat the query as a wildcard:
	 *  - ? represents any single character
	 *  - * represents any character sequence
	 * For faster results, it is recommended that the query text does not
	 * start with ? or *
	 */
	//TODO make it mutually exclusive with fuzzy use (but that's much more complex)
	TermCustomization wildcard();

	/**
	 * Create a Lucene query
	 */
	Query createQuery();

}
