/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.sort.comparatorsource.impl;

import java.io.IOException;

import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.GeoPointDistanceDocValues;
import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.DocValuesJoin;
import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.SortedNumericDoubleValues;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.index.SortedNumericDocValues;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.FieldComparator;
import org.apache.lucene.util.BitSet;

public class LuceneGeoPointDistanceComparatorSource extends LuceneFieldComparatorSource {

	private static final double MISSING_VALUE_IMPLICIT_DISTANCE_VALUE = Double.POSITIVE_INFINITY;

	private final double latitude;
	private final double longitude;

	public LuceneGeoPointDistanceComparatorSource(String nestedDocumentPath, double latitude, double longitude) {
		super( nestedDocumentPath );
		this.latitude = latitude;
		this.longitude = longitude;
	}

	@Override
	public FieldComparator<?> newComparator(String fieldname, int numHits, int sortPos, boolean reversed) {
		return new FieldComparator.DoubleComparator( numHits, fieldname, MISSING_VALUE_IMPLICIT_DISTANCE_VALUE ) {

			@Override
			protected NumericDocValues getNumericDocValues(LeafReaderContext context, String field) throws IOException {
				SortedNumericDocValues sortedNumericDocValues = context.reader().getSortedNumericDocValues( field );
				GeoPointDistanceDocValues geoPointDistanceDocValues = new GeoPointDistanceDocValues( sortedNumericDocValues, latitude, longitude );
				if ( nestedDocsProvider == null ) {
					return geoPointDistanceDocValues.getRawDoubleValues();
				}

				BitSet parentDocs = nestedDocsProvider.parentDocs( context );
				DocIdSetIterator childDocs = nestedDocsProvider.childDocs( context );
				if ( parentDocs == null || childDocs == null ) {
					return geoPointDistanceDocValues.getRawDoubleValues();
				}

				SortedNumericDoubleValues sortedNumericDoubleValues = SortedNumericDoubleValues.createDistance( geoPointDistanceDocValues );
				return DocValuesJoin.joinAsSingleValued( sortedNumericDoubleValues, Double.POSITIVE_INFINITY, parentDocs, childDocs ).getRawDoubleValues();
			}
		};
	}
}
