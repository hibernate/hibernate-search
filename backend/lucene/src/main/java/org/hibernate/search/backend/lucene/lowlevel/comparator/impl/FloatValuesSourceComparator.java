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
import org.apache.lucene.search.comparators.FloatComparator;

public class FloatValuesSourceComparator extends FloatComparator {

	private final DoubleMultiValuesToSingleValuesSource source;

	public FloatValuesSourceComparator(int numHits, String field, Float missingValue, boolean reversed, Pruning pruning,
			DoubleMultiValuesToSingleValuesSource source) {
		// Whether skipping is safe depends on the field, not on this class: it is only safe when our doc values
		// return the raw value, which LuceneNumericFieldComparatorSource decides before calling us. It cannot be
		// decided later, as the leaf comparator is fully initialized with a final skipping value.
		super( numHits, field, missingValue, reversed, pruning );
		this.source = source;
	}

	@Override
	public LeafFieldComparator getLeafComparator(LeafReaderContext context) throws IOException {
		return new FloatValuesSourceLeafComparator( context );
	}

	private class FloatValuesSourceLeafComparator extends FloatLeafComparator {
		FloatValuesSourceLeafComparator(LeafReaderContext context) throws IOException {
			super( context );
		}

		@Override
		protected NumericDocValues getNumericDocValues(LeafReaderContext context, String field) throws IOException {
			return source.getValues( context, null ).getRawFloatValues();
		}
	}

}
