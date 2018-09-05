/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.spatial.impl;

import static org.hibernate.search.spatial.impl.CoordinateHelper.coordinate;

import java.io.IOException;

import org.apache.lucene.index.DocValues;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.search.SimpleFieldComparator;
import org.apache.lucene.util.Bits;

//FIXME don't extend SimpleFieldComparator
public final class DistanceComparator extends SimpleFieldComparator<Double> {

	private final Point center;
	private final String latitudeField;
	private final String longitudeField;
	private double[] distances;
	private Bits docsWithLatitude;
	private Bits docsWithLongitude;
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
				distanceTo( doc )
		);
	}

	private double distanceTo(final int doc) {
		if ( docsWithLatitude.get( doc ) && docsWithLongitude.get( doc ) ) {
			return center.getDistanceTo(
					coordinate( latitudeValues, doc ),
					coordinate( longitudeValues, doc )
			);
		}
		else {
			return Double.MAX_VALUE;
		}
	}

	@Override
	public int compareTop(int doc) throws IOException {
		if ( topValue == null ) {
			return 1; //we consider any doc "higher" than null
		}

		final double distanceTo = distanceTo( doc );
		return Double.compare( distanceTo, topValue );
	}

	@Override
	public void copy(final int slot, final int doc) throws IOException {
		distances[slot] = distanceTo( doc );
	}

	@Override
	public void doSetNextReader(final LeafReaderContext context) throws IOException {
		final LeafReader atomicReader = context.reader();
		docsWithLatitude = DocValues.getDocsWithField( atomicReader, latitudeField );
		docsWithLongitude = DocValues.getDocsWithField( atomicReader, longitudeField );
		latitudeValues = DocValues.getNumeric( atomicReader, latitudeField );
		longitudeValues = DocValues.getNumeric( atomicReader, longitudeField );
	}

	@Override
	public Double value(final int slot) {
		return distances[slot];
	}
}
