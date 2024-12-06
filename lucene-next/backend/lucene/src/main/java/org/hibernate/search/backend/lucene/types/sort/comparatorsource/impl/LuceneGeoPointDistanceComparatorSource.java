/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.types.sort.comparatorsource.impl;

import org.hibernate.search.backend.lucene.lowlevel.comparator.impl.DoubleValuesSourceComparator;
import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.GeoPointDistanceMultiValuesToSingleValuesSource;
import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.MultiValueMode;
import org.hibernate.search.engine.spatial.GeoPoint;

import org.apache.lucene.search.FieldComparator;
import org.apache.lucene.search.Pruning;
import org.apache.lucene.search.Query;

public class LuceneGeoPointDistanceComparatorSource extends LuceneFieldComparatorSource {

	private final GeoPoint center;
	private final double missingValue;
	private final MultiValueMode mode;

	public LuceneGeoPointDistanceComparatorSource(String nestedDocumentPath, GeoPoint center, double missingValue,
			MultiValueMode mode, Query filter) {
		super( nestedDocumentPath, filter );
		this.center = center;
		this.missingValue = missingValue;
		this.mode = mode;
	}

	@Override
	public FieldComparator<?> newComparator(String fieldname, int numHits, Pruning pruning, boolean reversed) {
		GeoPointDistanceMultiValuesToSingleValuesSource source = new GeoPointDistanceMultiValuesToSingleValuesSource(
				fieldname, mode, nestedDocsProvider, center
		);
		// forcing to not skipping documents
		return new DoubleValuesSourceComparator( numHits, fieldname, missingValue, reversed, Pruning.NONE, source );
	}
}
