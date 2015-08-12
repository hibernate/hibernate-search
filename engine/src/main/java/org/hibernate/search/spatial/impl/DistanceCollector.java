/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.spatial.impl;

import static org.hibernate.search.spatial.impl.CoordinateHelper.coordinate;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.LeafCollector;
import org.apache.lucene.search.Scorer;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.spatial.Coordinates;

/**
 * A Lucene distance {@code Collector} for spatial searches.
 *
 * @author Sanne Grinovero
 * @author Nicolas Helleringer
 */
public class DistanceCollector implements Collector {

	private final Point center;
	private final SpatialResultsCollector distances;
	private final String latitudeField;
	private final String longitudeField;

	public DistanceCollector(Coordinates centerCoordinates, int hitsCount, String fieldname) {
		this.center = Point.fromCoordinates( centerCoordinates );
		this.distances = new SpatialResultsCollector( hitsCount );
		this.latitudeField = SpatialHelper.formatLatitude( fieldname );
		this.longitudeField = SpatialHelper.formatLongitude( fieldname );
	}

	public double getDistance(final int index) {
		return distances.get( index, center );
	}

	@Override
	public LeafCollector getLeafCollector(LeafReaderContext context) throws IOException {
		return new DistanceLeafCollector( context );
	}

	@Override
	public boolean needsScores() {
		return false;
	}

	/**
	 * We'll store matching hits in HitEntry instances so to allow retrieving results
	 * in a second phase after the Collector has run.
	 * Also take the opportunity to lazily calculate the actual distance: only store
	 * latitude and longitude coordinates.
	 */
	private static class HitEntry {
		int documentId;
		double latitude;
		double longitude;

		private HitEntry(int documentId, double latitude, double longitude) {
			this.documentId = documentId;
			this.latitude = latitude;
			this.longitude = longitude;
		}

		double getDistance(final Point center) {
			return center.getDistanceTo( latitude, longitude );
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
			orderedEntries = new ArrayList<HitEntry>( size );
		}

		public double get(int index, Point center) {
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
			throw new SearchException( "Unexpected index: this documentId was not collected" );
		}

		void put(int documentId, double latitude, double longitude) {
			orderedEntries.add( new HitEntry( documentId, latitude, longitude ) );
		}
	}

	private class DistanceLeafCollector implements LeafCollector {

		private final int docBase;
		private final NumericDocValues latitudeValues;
		private final NumericDocValues longitudeValues;

		DistanceLeafCollector(LeafReaderContext context) throws IOException {
			final LeafReader atomicReader = context.reader();
			this.latitudeValues = atomicReader.getNumericDocValues( latitudeField );
			this.longitudeValues = atomicReader.getNumericDocValues( longitudeField );
			this.docBase = context.docBase;
		}

		@Override
		public void setScorer(Scorer scorer) throws IOException {
		}

		@Override
		public void collect(int doc) throws IOException {
			final int absolute = docBase + doc;
			double lat = coordinate( latitudeValues, doc );
			double lon = coordinate( longitudeValues, doc );
			distances.put( absolute, lat, lon );
		}
	}
}
