/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
import org.hibernate.search.engine.search.query.SearchResultTotal;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class SimpleSearchResult<H> implements SearchResult<H> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final SearchResultTotal resultTotal;
	private final List<H> hits;
	private final Map<AggregationKey<?>, ?> aggregationResults;
	private final Duration took;
	private final boolean timedOut;

	public SimpleSearchResult(SearchResultTotal resultTotal, List<H> hits, Map<AggregationKey<?>, ?> aggregationResults,
			Duration took, Boolean timedOut) {
		this.resultTotal = resultTotal;
		this.hits = hits;
		this.aggregationResults = aggregationResults;
		this.took = took;
		this.timedOut = ( timedOut != null ) && timedOut;
	}

	@Override
	public SearchResultTotal total() {
		return resultTotal;
	}

	@Override
	public List<H> hits() {
		return hits;
	}

	@Override
	@SuppressWarnings("unchecked") // The type of aggregation results must be consistent with the type of keys, by contract
	public <T> T aggregation(AggregationKey<T> key) {
		Object aggregationResult = aggregationResults.get( key );
		if ( aggregationResult == null && !aggregationResults.containsKey( key ) ) {
			throw log.unknownAggregationKey( key );
		}
		return (T) aggregationResult;
	}

	@Override
	public Duration took() {
		return took;
	}

	@Override
	public boolean timedOut() {
		return timedOut;
	}

	@Override
	public String toString() {
		return new StringJoiner( ", ", SimpleSearchResult.class.getSimpleName() + "[", "]" )
				.add( "resultTotal=" + resultTotal )
				.add( "hits=" + hits )
				.add( "aggregationResults=" + aggregationResults )
				.add( "took=" + took )
				.add( "timedOut=" + timedOut )
				.toString();
	}
}
