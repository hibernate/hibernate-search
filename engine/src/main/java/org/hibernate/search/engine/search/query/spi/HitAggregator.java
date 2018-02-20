/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.query.spi;

/**
 * The class responsible for aggregating hits when running a search query.
 * <p>
 * Provides one collector per hit, and builds the aggregated result
 * when all hits have been processed.
 *
 * @param <C> The hit collector type. See {@link DocumentReferenceHitCollector}, {@link LoadingHitCollector}
 * and {@link ProjectionHitCollector} in particular.
 * @param <T> The type of the aggregated result.
 */
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
