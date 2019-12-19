/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.sort.comparatorsource.impl;

import java.io.IOException;

import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.DocValuesJoin;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.search.FieldComparator;

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
				return DocValuesJoin.getJoinedAsSingleValuedDistance(
						context, field, nestedDocsProvider,
						latitude, longitude,
						Double.POSITIVE_INFINITY
				)
						.getRawDoubleValues();
			}
		};
	}
}
