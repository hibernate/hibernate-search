/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.query;

import java.util.concurrent.TimeUnit;

import org.hibernate.search.engine.search.query.dsl.SearchQueryOptionsStep;

/**
 * Result related to all matching documents: ether belonging to the current page or not.
 * It contains the exact or a lower bound of the total number of matching entities.
 */
public interface SearchResultTotal {

	/**
	 * @return whether the total hit count is exact.
	 */
	boolean isHitCountExact();

	/**
	 * It could be a lower bound only when a {@link SearchQueryOptionsStep#totalHitsThreshold(int)} or
	 * a {@link SearchQueryOptionsStep#truncateAfter(long, TimeUnit)} has been defined
	 * for the current query.
	 *
	 * @return whether the total hit count is lower bound.
	 */
	boolean isHitCountLowerBound();

	/**
	 * @return the exact value of the total hit count.
	 * @throws org.hibernate.search.util.common.SearchException if {@link #isHitCountLowerBound()}
	 */
	long hitCount();

	/**
	 * Get the total hit count value.
	 * The exact value is a (not-strict) lower bound value too,
	 * the opposite is false.
	 *
	 * @return the total hit count (lower bound or exact)
	 */
	long hitCountLowerBound();

}
