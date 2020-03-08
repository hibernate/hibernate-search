/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.docvalues.impl;

import java.io.IOException;

import org.apache.lucene.geo.GeoEncodingUtils;
import org.apache.lucene.index.SortedNumericDocValues;
import org.apache.lucene.util.SloppyMath;

public class GeoPointDistanceDocValues extends DoubleMultiValues {

	private final SortedNumericDocValues values;
	private final double latitude;
	private final double longitude;

	public GeoPointDistanceDocValues(SortedNumericDocValues values, double latitude, double longitude) {
		this.values = values;
		this.latitude = latitude;
		this.longitude = longitude;
	}

	@Override
	public double doubleValue() throws IOException {
		long encoded = values.nextValue();
		double valueLatitude = GeoEncodingUtils.decodeLatitude( (int) ( encoded >>> 32 ) );
		double valueLongitude = GeoEncodingUtils.decodeLongitude( (int) ( encoded ) );

		return SloppyMath.haversinMeters( latitude, longitude, valueLatitude, valueLongitude );
	}

	@Override
	public boolean advanceExact(int doc) throws IOException {
		return values.advanceExact( doc );
	}
}
