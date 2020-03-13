/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.docvalues.impl;

import java.io.IOException;

import org.hibernate.search.engine.spatial.GeoPoint;

import org.apache.lucene.geo.GeoEncodingUtils;
import org.apache.lucene.index.SortedNumericDocValues;
import org.apache.lucene.util.SloppyMath;

public class GeoPointDistanceDocValues extends SortedNumericDoubleDocValues {

	private final SortedNumericDocValues values;
	private final double latitude;
	private final double longitude;

	public GeoPointDistanceDocValues(SortedNumericDocValues values, GeoPoint center) {
		this.values = values;
		this.latitude = center.getLatitude();
		this.longitude = center.getLongitude();
	}

	@Override
	public double nextValue() throws IOException {
		long encoded = values.nextValue();
		double valueLatitude = GeoEncodingUtils.decodeLatitude( (int) ( encoded >>> 32 ) );
		double valueLongitude = GeoEncodingUtils.decodeLongitude( (int) ( encoded ) );

		return SloppyMath.haversinMeters( latitude, longitude, valueLatitude, valueLongitude );
	}

	@Override
	public int docValueCount() {
		return values.docValueCount();
	}

	@Override
	public boolean advanceExact(int doc) throws IOException {
		// TODO HSEARCH-3103 in order to support multi-values here,
		//  we must sort the distances before returning them through nextValue().
		return values.advanceExact( doc );
	}

	@Override
	public int docID() {
		return values.docID();
	}

	@Override
	public int nextDoc() throws IOException {
		return values.nextDoc();
	}

	@Override
	public int advance(int target) throws IOException {
		return values.advance( target );
	}

	@Override
	public long cost() {
		return values.cost();
	}
}
