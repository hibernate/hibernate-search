/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.comparator.impl;

import java.io.IOException;

import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.DoubleMultiValuesToSingleValuesSource;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.search.LeafFieldComparator;
import org.apache.lucene.search.comparators.FloatComparator;

public class FloatValuesSourceComparator extends FloatComparator {

	private final DoubleMultiValuesToSingleValuesSource source;

	public FloatValuesSourceComparator(int numHits, String field, Float missingValue, boolean reversed, boolean enableSkipping,
			DoubleMultiValuesToSingleValuesSource source) {
		super( numHits, field, missingValue, reversed, enableSkipping );
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
