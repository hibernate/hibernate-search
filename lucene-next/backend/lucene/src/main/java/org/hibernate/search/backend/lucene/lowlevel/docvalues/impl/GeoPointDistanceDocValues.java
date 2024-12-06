/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.lowlevel.docvalues.impl;

import java.io.IOException;
import java.util.Arrays;

import org.hibernate.search.engine.spatial.GeoPoint;

import org.apache.lucene.geo.GeoEncodingUtils;
import org.apache.lucene.index.SortedNumericDocValues;
import org.apache.lucene.util.SloppyMath;

public class GeoPointDistanceDocValues extends SortedNumericDoubleDocValues {

	private final SortedNumericDocValues values;
	private final double latitude;
	private final double longitude;

	private double[] distances = new double[1];
	private int distanceIndex;

	public GeoPointDistanceDocValues(SortedNumericDocValues values, GeoPoint center) {
		this.values = values;
		this.latitude = center.latitude();
		this.longitude = center.longitude();
	}

	@Override
	public double nextValue() throws IOException {
		setDistancesIfNecessary();
		return distances[distanceIndex++];
	}

	@Override
	public int docValueCount() {
		return values.docValueCount();
	}

	@Override
	public boolean advanceExact(int doc) throws IOException {
		distanceIndex = -1;
		return values.advanceExact( doc );
	}

	@Override
	public int docID() {
		return values.docID();
	}

	@Override
	public int nextDoc() throws IOException {
		distanceIndex = -1;
		return values.nextDoc();
	}

	@Override
	public int advance(int target) throws IOException {
		distanceIndex = -1;
		return values.advance( target );
	}

	@Override
	public long cost() {
		return values.cost();
	}

	private void setDistancesIfNecessary() throws IOException {
		if ( distanceIndex >= 0 ) {
			return;
		}

		int count = values.docValueCount();
		if ( distances.length < count ) {
			distances = new double[count];
		}
		for ( int i = 0; i < count; i++ ) {
			long encodedPoint = values.nextValue();
			double pointLatitude = GeoEncodingUtils.decodeLatitude( (int) ( encodedPoint >>> 32 ) );
			double pointLongitude = GeoEncodingUtils.decodeLongitude( (int) ( encodedPoint ) );
			distances[i] = SloppyMath.haversinMeters( latitude, longitude, pointLatitude, pointLongitude );
		}
		// By contract, values must be returned in ascending order.
		Arrays.sort( distances, 0, count );

		distanceIndex = 0;
	}
}
