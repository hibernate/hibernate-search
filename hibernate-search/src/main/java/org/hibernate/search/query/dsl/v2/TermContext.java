package org.hibernate.search.query.dsl.v2;

import org.apache.lucene.search.Query;

import org.hibernate.search.query.dsl.v2.QueryBuilder;

/**
 * @author Emmanuel Bernard
 */
public interface TermContext {
	/**
	 * field / property the term query is executed on
	 */
	TermMatchingContext on(String field);

	interface TermMatchingContext {
		/**
		 * text searched in the term query (the term is pre-analyzer unless ignoreAnalyzer is called)
		 */
		TermCustomization matches(String text);
	}

	interface TermCustomization {
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

		interface TermFuzzy extends TermCustomization {
			/**
			 * Threshold above which two terms are considered similar enough.
			 * Value between 0 and 1 (1 excluded)
			 * Defaults to .5
			 */
			TermFuzzy threshold(float threshold);

			/**
			 * Size of the prefix ignored by the fuzzyness.
			 * A non zero value is recommended if the index contains a huge amount of distinct terms
			 *
			 * Defaults to 0
			 */
			TermFuzzy prefixLength(int prefixLength);
		}
	}
}
