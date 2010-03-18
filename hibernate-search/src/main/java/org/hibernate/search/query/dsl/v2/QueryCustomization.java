package org.hibernate.search.query.dsl.v2;

import org.apache.lucene.search.Filter;
import org.apache.lucene.search.Query;

/**
 * Operations common to all types of queries
 *
 * @author Emmanuel Bernard
 */
public interface QueryCustomization<T> {

	/**
	 * Boost the query to a given value
	 * Most of the time positive float:
	 *  - lower than 1 to diminish the weight
	 *  - higher than 1 to increase the weight
	 *
	 * Could be negative but not unless you understand what is going on (advanced)
	 */
	T boostedTo(float boost);

	/**
	 * All results matching the query have a constant score equals to the boost
	 * FIXME is that true?
	 */
	T constantScore();

	/**
	 * Filter the query results with the Filter instance
	 */
	T filter(Filter filter);

	//TODO filter(String) + parameters

	/**
	 * Create a Lucene query
	 */
	Query createQuery();
}
