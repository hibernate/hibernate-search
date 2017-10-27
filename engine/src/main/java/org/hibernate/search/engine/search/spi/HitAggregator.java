/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.spi;

public interface HitAggregator<C, T> {

	/**
	 * Initialize the aggregator for a new aggregation.
	 */
	void init(int expectedHitCount);

	/**
	 * @return A collector to use for the next hit.
	 */
	C nextCollector();

	/**
	 * @return The aggregated result.
	 */
	T build();

}
