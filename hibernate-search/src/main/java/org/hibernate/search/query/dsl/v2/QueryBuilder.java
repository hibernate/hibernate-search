package org.hibernate.search.query.dsl.v2;

import org.apache.lucene.search.Query;

/**
 * @author Emmanuel Bernard
 */
public interface QueryBuilder {
	/**
	 * build a term query
	 */
	TermContext keyword();

	/**
	 * find matching elements within a range
	 */
	RangeContext range();

	/**
	 * find an sentence (words can be inversed according to the slop factor
	 */
	PhraseContext phrase();

	/**
	 * Boolean query
	 */
	BooleanJunction<BooleanJunction> bool();

	/**
	 * Query matching all documents
	 * Typically mixed with a boolean query.
	 */
	AllContext all();
}
