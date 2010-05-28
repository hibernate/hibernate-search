package org.hibernate.search.query.dsl.v2;

import org.apache.lucene.search.Query;

/**
 * @author Emmanuel Bernard
 */
public interface QueryBuilder {
	/**
	 * build a term query
	 */
	TermContext exact();

	/**
	 * Use a fuzzy search approximation (aka edit distance)
	 */
	FuzzyContext fuzzy();

	/**
	 * Treat the query as a wildcard:
	 *  - ? represents any single character
	 *  - * represents any character sequence
	 * For faster results, it is recommended that the query text does not
	 * start with ? or *
	 */
	WildcardContext wildcard();

	/**
	 * Boolean query
	 */
	BooleanJunction<BooleanJunction> bool();

	/**
	 * find matching elements within a range
	 */
	RangeContext range();

	/**
	 * find an sentence (words can be inversed according to the slop factor
	 */
	PhraseContext phrase();

	/**
	 * Query matching all documents
	 * Typically mixed with a boolean query.
	 */
	AllContext all();
}
