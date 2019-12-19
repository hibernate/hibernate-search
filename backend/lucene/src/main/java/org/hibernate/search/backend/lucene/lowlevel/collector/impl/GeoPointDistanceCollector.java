/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.collector.impl;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.DoubleValues;
import org.apache.lucene.search.LeafCollector;
import org.apache.lucene.search.Scorable;
import org.apache.lucene.search.ScoreMode;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.DocValuesJoin;
import org.hibernate.search.backend.lucene.lowlevel.join.impl.NestedDocsProvider;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * A Lucene distance {@code Collector} for spatial searches.
 *
 * @author Sanne Grinovero
 * @author Nicolas Helleringer
 */
public class GeoPointDistanceCollector implements Collector {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final double MISSING_VALUE_MARKER = Double.NEGATIVE_INFINITY;

	private final String absoluteFieldPath;
	private final NestedDocsProvider nestedDocsProvider;
	private final GeoPoint center;

	private final SpatialResultsCollector distances;

	public GeoPointDistanceCollector(String absoluteFieldPath, NestedDocsProvider nestedDocsProvider,
			GeoPoint center, int hitsCount) {
		this.absoluteFieldPath = absoluteFieldPath;
		this.nestedDocsProvider = nestedDocsProvider;
		this.center = center;
		this.distances = new SpatialResultsCollector( hitsCount );
	}

	public Double getDistance(final int docId) {
		return distances.get( docId );
	}

	@Override
	public LeafCollector getLeafCollector(LeafReaderContext context) throws IOException {
		return new DistanceLeafCollector( context.docBase, createDistanceDocValues( context ) );
	}

	@Override
	public ScoreMode scoreMode() {
		return ScoreMode.COMPLETE_NO_SCORES;
	}

	private DoubleValues createDistanceDocValues(LeafReaderContext context) throws IOException {
		return DocValuesJoin.getJoinedAsSingleValuedDistance(
				context, absoluteFieldPath, nestedDocsProvider,
				center.getLatitude(), center.getLongitude(),
				MISSING_VALUE_MARKER
		);
	}

	/**
	 * We'll store matching hits in HitEntry instances so to allow retrieving results
	 * in a second phase after the Collector has run.
	 */
	private static class HitEntry {
		private final int documentId;
		private final Double distance;

		private HitEntry(int documentId, Double distance) {
			this.documentId = documentId;
			this.distance = distance;
		}
	}

	/**
	 * A custom structure to store all HitEntry instances.
	 * Based on an array, as in most cases we'll append sequentially and iterate the
	 * results in the same order too. The size is well known in most situations so we can
	 * also guess an appropriate allocation size.
	 *
	 * Iteration of the results will in practice follow a monotonic index, unless a non natural
	 * Sort is specified. So by keeping track of the position in the array of the last request,
	 * and look from that pointer first, the cost of get operations is O(1) in most common use
	 * cases.
	 */
	private static class SpatialResultsCollector {
		final ArrayList<HitEntry> orderedEntries;
		int currentIterator = 0;

		private SpatialResultsCollector(int size) {
			orderedEntries = new ArrayList<>( size );
		}

		public Double get(int index) {
			//Optimize for an iteration having a monotonic index:
			int startingPoint = currentIterator;
			for ( ; currentIterator < orderedEntries.size(); currentIterator++ ) {
				HitEntry currentEntry = orderedEntries.get( currentIterator );
				if ( currentEntry == null ) {
					break;
				}
				if ( currentEntry.documentId == index ) {
					return currentEntry.distance;
				}
			}

			//No match yet! scan the remaining section from the beginning:
			for ( currentIterator = 0; currentIterator < startingPoint; currentIterator++ ) {
				HitEntry currentEntry = orderedEntries.get( currentIterator );
				if ( currentEntry == null ) {
					break;
				}
				if ( currentEntry.documentId == index ) {
					return currentEntry.distance;
				}
			}

			throw log.documentIdNotCollected( index );
		}

		void put(int documentId, Double distance) {
			orderedEntries.add( new HitEntry( documentId, distance ) );
		}
	}

	private class DistanceLeafCollector implements LeafCollector {

		private final int docBase;
		private final DoubleValues distanceDocValues;

		DistanceLeafCollector(int docBase, DoubleValues distanceDocValues) {
			this.docBase = docBase;
			this.distanceDocValues = distanceDocValues;
		}

		@Override
		public void setScorer(Scorable scorer) {
			// we don't need any scorer
		}

		@Override
		public void collect(int docId) throws IOException {
			final int absoluteDocId = docBase + docId;
			Double distance = null;
			if ( distanceDocValues.advanceExact( docId ) ) {
				double distanceFromDocValues = distanceDocValues.doubleValue();
				distance = distanceFromDocValues == MISSING_VALUE_MARKER ? null : distanceFromDocValues;
			}
			distances.put( absoluteDocId, distance );
		}
	}
}
