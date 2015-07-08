/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.spatial.impl;

import static org.hibernate.search.spatial.impl.CoordinateHelper.coordinate;

import java.io.IOException;

import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.search.SimpleFieldComparator;

//FIXME don't extend SimpleFieldComparator
public final class DistanceComparator extends SimpleFieldComparator<Double> {

	private final Point center;
	private final String latitudeField;
	private final String longitudeField;
	private double[] distances;
	private NumericDocValues latitudeValues;
	private NumericDocValues longitudeValues;
	private Double bottomDistance;
	private Double topValue;

	public DistanceComparator(Point center, int numHits, String fieldName) {
		this.center = center;
		this.distances = new double[numHits];
		this.latitudeField = SpatialHelper.formatLatitude( fieldName );
		this.longitudeField = SpatialHelper.formatLongitude( fieldName );
	}

	@Override
	public int compare(final int slot1, final int slot2) {
		return Double.compare( distances[slot1], distances[slot2] );
	}

	@Override
	public void setBottom(final int slot) {
		bottomDistance = distances[slot];
	}

	@Override
	public void setTopValue(Double value) {
		topValue = value;
	}

	@Override
	public int compareBottom(final int doc) throws IOException {
		return Double.compare(
				bottomDistance,
				center.getDistanceTo( latitude( doc ), longitude( doc ) )
		);
	}

	private double longitude(final int doc) {
		return coordinate( longitudeValues, doc );
	}

	private double latitude(final int doc) {
		return coordinate( latitudeValues, doc );
	}

	@Override
	public int compareTop(int doc) throws IOException {
		if ( topValue == null ) {
			return 1; //we consider any doc "higher" than null
		}

		final double distanceTo = center.getDistanceTo( latitude( doc ), longitude( doc ) );
		return Double.compare( distanceTo, topValue );
	}

	@Override
	public void copy(final int slot, final int doc) throws IOException {
		distances[slot] = center.getDistanceTo(
				latitude( doc ),
				longitude( doc )
		);
	}

	@Override
	public void doSetNextReader(final LeafReaderContext context) throws IOException {
		final LeafReader atomicReader = context.reader();
		latitudeValues = atomicReader.getNumericDocValues( latitudeField );
		longitudeValues = atomicReader.getNumericDocValues( longitudeField );
	}

	@Override
	public Double value(final int slot) {
		return distances[slot];
	}
}
