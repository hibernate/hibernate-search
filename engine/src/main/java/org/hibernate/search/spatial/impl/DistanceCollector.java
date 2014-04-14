/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2012-2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.search.spatial.impl;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.FieldCache;
import org.apache.lucene.search.FieldCache.Doubles;
import org.apache.lucene.search.Scorer;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.spatial.Coordinates;

/**
 * A Lucene distance {@code Collector} for spatial searches.
 *
 * @author Sanne Grinovero
 * @author Nicolas Helleringer
 */
public class DistanceCollector extends Collector {

	private final Point center;
	private final boolean acceptsDocsOutOfOrder;
	private final Collector delegate;
	private final SpatialResultsCollector distances;
	private final String latitudeField;
	private final String longitudeField;

	private int docBase = 0;
	private Doubles latitudeValues;
	private Doubles longitudeValues;

	public DistanceCollector(Collector delegate, Coordinates centerCoordinates, int hitsCount, String fieldname) {
		this.delegate = delegate;
		this.acceptsDocsOutOfOrder = delegate.acceptsDocsOutOfOrder();
		this.center = Point.fromCoordinates( centerCoordinates );
		this.distances = new SpatialResultsCollector( hitsCount );
		this.latitudeField = SpatialHelper.formatLatitude( fieldname );
		this.longitudeField = SpatialHelper.formatLongitude( fieldname );
	}

	@Override
	public void setScorer(final Scorer scorer) throws IOException {
		delegate.setScorer( scorer );
	}

	@Override
	public void collect(final int doc) throws IOException {
		delegate.collect( doc );
		final int absolute = docBase + doc;
		distances.put( absolute, latitudeValues.get( doc ), longitudeValues.get( doc ) );
	}

	@Override
	public void setNextReader(AtomicReaderContext newContext) throws IOException {
		delegate.setNextReader( newContext );
		final AtomicReader atomicReader = newContext.reader();
		latitudeValues = FieldCache.DEFAULT.getDoubles( atomicReader, latitudeField, false );
		longitudeValues = FieldCache.DEFAULT.getDoubles( atomicReader, longitudeField, false );
		this.docBase = newContext.docBase;
	}

	@Override
	public boolean acceptsDocsOutOfOrder() {
		return acceptsDocsOutOfOrder;
	}

	public double getDistance(final int index) {
		return distances.get( index, center );
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

}
