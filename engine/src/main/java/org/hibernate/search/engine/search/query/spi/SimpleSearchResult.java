/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.query.spi;

import java.lang.invoke.MethodHandles;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class SimpleSearchResult<H> implements SearchResult<H> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final long hitCount;
	private final List<H> hits;
	private final Map<AggregationKey<?>, ?> aggregationResults;
	private final Duration took;
	private final boolean timedOut;

	public SimpleSearchResult(long hitCount, List<H> hits, Map<AggregationKey<?>, ?> aggregationResults,
			Integer took, Boolean timedOut) {
		this.hitCount = hitCount;
		this.hits = hits;
		this.aggregationResults = aggregationResults;
		this.took = ( took == null ) ? null : Duration.ofMillis( took );
		this.timedOut = ( timedOut != null ) && timedOut;
	}

	@Override
	public long getTotalHitCount() {
		return hitCount;
	}

	@Override
	public List<H> getHits() {
		return hits;
	}

	@Override
	@SuppressWarnings("unchecked") // The type of aggregation results must be consistent with the type of keys, by contract
	public <T> T getAggregation(AggregationKey<T> key) {
		Object aggregationResult = aggregationResults.get( key );
		if ( aggregationResult == null && !aggregationResults.containsKey( key ) ) {
			throw log.unknownAggregationKey( key );
		}
		return (T) aggregationResult;
	}

	@Override
	public Duration getTook() {
		return took;
	}

	@Override
	public boolean isTimedOut() {
		return timedOut;
	}

	@Override
	public String toString() {
		return new StringJoiner( ", ", SimpleSearchResult.class.getSimpleName() + "[", "]" )
				.add( "hitCount=" + hitCount )
				.add( "hits=" + hits )
				.add( "aggregationResults=" + aggregationResults )
				.add( "took=" + took )
				.add( "timedOut=" + timedOut )
				.toString();
	}
}
