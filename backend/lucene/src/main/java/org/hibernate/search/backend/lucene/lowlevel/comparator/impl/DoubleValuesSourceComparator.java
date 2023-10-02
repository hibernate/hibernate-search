/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.lowlevel.comparator.impl;

import java.io.IOException;

import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.DoubleMultiValuesToSingleValuesSource;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.search.LeafFieldComparator;
import org.apache.lucene.search.Pruning;
import org.apache.lucene.search.comparators.DoubleComparator;

public class DoubleValuesSourceComparator extends DoubleComparator {

	private final DoubleMultiValuesToSingleValuesSource source;

	public DoubleValuesSourceComparator(int numHits, String field, Double missingValue, boolean reversed,
			Pruning ignored, DoubleMultiValuesToSingleValuesSource source) {
		// See Javadocs for org.apache.lucene.search.comparators.NumericComparator.NumericLeafComparator#getNumericDocValues(LeafReaderContext, String)
		// >>  * If you override this method, you should probably always disable skipping as the comparator
		//     * uses values from the points index to build its competitive iterators, and assumes that the
		//     * values in doc values and points are the same.
		// Disabling skipping in the leaf comparator is already too late as the leaf comparator is fully initialized and final skipping value is already set.
		super( numHits, field, missingValue, reversed, Pruning.NONE );
		this.source = source;
	}

	@Override
	public LeafFieldComparator getLeafComparator(LeafReaderContext context) throws IOException {
		return new DoubleValuesSourceLeafComparator( context );
	}

	private class DoubleValuesSourceLeafComparator extends DoubleLeafComparator {
		DoubleValuesSourceLeafComparator(LeafReaderContext context) throws IOException {
			super( context );
		}

		@Override
		protected NumericDocValues getNumericDocValues(LeafReaderContext context, String field) throws IOException {
			return source.getValues( context, null ).getRawDoubleValues();
		}
	}

}
