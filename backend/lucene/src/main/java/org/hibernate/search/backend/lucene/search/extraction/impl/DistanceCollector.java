/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.extraction.impl;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Set;

import org.apache.lucene.geo.GeoEncodingUtils;
import org.apache.lucene.index.DocValues;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.SortedNumericDocValues;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.LeafCollector;
import org.apache.lucene.search.Scorable;
import org.apache.lucene.search.ScoreMode;
import org.apache.lucene.util.SloppyMath;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * A Lucene distance {@code Collector} for spatial searches.
 *
 * @author Sanne Grinovero
 * @author Nicolas Helleringer
 */
public class DistanceCollector implements Collector {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final String absoluteFieldPath;
	private final GeoPoint center;
	private final SpatialResultsCollector distances;

	public DistanceCollector(String absoluteFieldPath, GeoPoint center, int hitsCount) {
		this.center = center;
		this.absoluteFieldPath = absoluteFieldPath;
		this.distances = new SpatialResultsCollector( hitsCount );
	}

	public Double getDistance(final int docId, LuceneCollectorExtractContext context) {
		Double result = distances.get( docId, center );
		if ( result != null ) {
			return result;
		}

		// try to find the field on nested docs
		Set<Integer> nestedDocs = context.getNestedDocIds( docId );
		if ( nestedDocs == null ) {
			return null;
		}
		for ( Integer nestedDocId : nestedDocs ) {
			result = distances.get( nestedDocId, center );
			if ( result != null ) {
				return result;
			}
		}

		return null;
	}

	@Override
	public LeafCollector getLeafCollector(LeafReaderContext context) throws IOException {
		return new DistanceLeafCollector( context );
	}

	@Override
	public ScoreMode scoreMode() {
		return ScoreMode.COMPLETE_NO_SCORES;
	}

	/**
	 * We'll store matching hits in HitEntry instances so to allow retrieving results
	 * in a second phase after the Collector has run.
	 * Also take the opportunity to lazily calculate the actual distance: only store
	 * latitude and longitude coordinates.
	 */
	private abstract static class HitEntry {
		private final int documentId;

		private HitEntry(int documentId) {
			this.documentId = documentId;
		}

		public abstract Double getDistance(GeoPoint center);
	}

	private static final class CompleteHitEntry extends HitEntry {
		private final double latitude;
		private final double longitude;

		private CompleteHitEntry(int documentId, double latitude, double longitude) {
			super( documentId );
			this.latitude = latitude;
			this.longitude = longitude;
		}

		@Override
		public Double getDistance(final GeoPoint center) {
			return SloppyMath.haversinMeters( center.getLatitude(), center.getLongitude(), latitude, longitude );
		}
	}

	private static final class IncompleteHitEntry extends HitEntry {
		private IncompleteHitEntry(int documentId) {
			super( documentId );
		}

		@Override
		public Double getDistance(final GeoPoint center) {
			return null;
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

		public Double get(int index, GeoPoint center) {
			//Optimize for an iteration having a monotonic index:
			int startingPoint = currentIterator;
			for ( ; currentIterator < orderedEntries.size(); currentIterator++ ) {
				HitEntry currentEntry = orderedEntries.get( currentIterator );
				if ( currentEntry == null ) {
					break;
				}
				if ( currentEntry.documentId == index ) {
					return currentEntry.getDistance( center );
				}
			}

			//No match yet! scan the remaining section from the beginning:
			for ( currentIterator = 0; currentIterator < startingPoint; currentIterator++ ) {
				HitEntry currentEntry = orderedEntries.get( currentIterator );
				if ( currentEntry == null ) {
					break;
				}
				if ( currentEntry.documentId == index ) {
					return currentEntry.getDistance( center );
				}
			}

			throw log.documentIdNotCollected( index );
		}

		void put(int documentId, double latitude, double longitude) {
			orderedEntries.add( new CompleteHitEntry( documentId, latitude, longitude ) );
		}

		void putIncomplete(int documentId) {
			orderedEntries.add( new IncompleteHitEntry( documentId ) );
		}
	}

	private class DistanceLeafCollector implements LeafCollector {

		private final int docBase;
		private final SortedNumericDocValues geoPointValues;

		DistanceLeafCollector(LeafReaderContext context) throws IOException {
			final LeafReader atomicReader = context.reader();
			this.geoPointValues = DocValues.getSortedNumeric( atomicReader, absoluteFieldPath );
			this.docBase = context.docBase;
		}

		@Override
		public void setScorer(Scorable scorer) {
			// we don't need any scorer
		}

		@Override
		public void collect(int doc) throws IOException {
			final int absolute = docBase + doc;
			if ( geoPointValues.advanceExact( doc ) ) {
				long encodedValue = geoPointValues.nextValue();
				double latitude = GeoEncodingUtils.decodeLatitude( (int) ( encodedValue >> 32 ) );
				double longitude = GeoEncodingUtils.decodeLongitude( (int) encodedValue );
				distances.put( absolute, latitude, longitude );
			}
			else {
				distances.putIncomplete( absolute );
			}
		}
	}
}
