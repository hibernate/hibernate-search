/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.query.dsl;

import org.apache.lucene.search.Filter;

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
	 * @param boost the value to use as boost
	 * @return an instance of T for method chaining
	 */
	T boostedTo(float boost);

	/**
	 * All results matching the query have a constant score equals to the boost
	 * FIXME is that true?
	 * @return an instance of T for method chaining
	 */
	T withConstantScore();

	/**
	 * Filter the query results with the Filter instance
	 * @param filter the filter to use
	 * @return an instance of T for method chaining
	 */
	T filteredBy(Filter filter);

	//TODO filter(String) + parameters
}
