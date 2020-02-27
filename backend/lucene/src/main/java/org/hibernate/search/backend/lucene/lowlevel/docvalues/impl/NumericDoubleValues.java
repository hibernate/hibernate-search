/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.docvalues.impl;

import java.io.IOException;
import java.util.function.DoubleToLongFunction;

import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.search.DoubleValues;

/**
 * A per-document numeric value.
 * <p>
 * Copied and adapted from {@code org.elasticsearch.index.fielddata.NumericDoubleValues} class
 * of <a href="https://github.com/elastic/elasticsearch">Elasticsearch project</a>.
 */
public abstract class NumericDoubleValues extends DoubleValues {

	/**
	 * Sole constructor. (For invocation by subclass
	 * constructors, typically implicit.)
	 */
	protected NumericDoubleValues() {
	}

	/**
	 * Returns numeric docvalues view of raw double bits
	 * @return numeric
	 */
	public NumericDocValues getRawDoubleValues() {
		return new RawNumericDocValues( Double::doubleToRawLongBits );
	}

	/**
	 * Returns numeric docvalues view of raw float bits
	 * @return numeric
	 */
	public NumericDocValues getRawFloatValues() {
		return new RawNumericDocValues( (v) -> (long) Float.floatToRawIntBits( (float) v ) );
	}

	/**
	 * Returns numeric docvalues view of raw long bits
	 * @return numeric
	 */
	public NumericDocValues getRawLongValues() {
		return new RawNumericDocValues( (v) -> (long) v );
	}

	/**
	 * Returns numeric docvalues view of raw int bits
	 * @return numeric
	 */
	public NumericDocValues getRawIntValues() {
		return getRawLongValues();
	}

	private class RawNumericDocValues extends NumericDocValues {
		private int docID = -1;
		private final DoubleToLongFunction decorator;

		public RawNumericDocValues(DoubleToLongFunction decorator) {
			this.decorator = decorator;
		}

		@Override
		public boolean advanceExact(int target) throws IOException {
			docID = target;
			return NumericDoubleValues.this.advanceExact( target );
		}

		@Override
		public long longValue() throws IOException {
			return decorator.applyAsLong( NumericDoubleValues.this.doubleValue() );
		}

		@Override
		public int docID() {
			return docID;
		}

		@Override
		public int nextDoc() {
			throw new UnsupportedOperationException();
		}

		@Override
		public int advance(int target) {
			throw new UnsupportedOperationException();
		}

		@Override
		public long cost() {
			throw new UnsupportedOperationException();
		}
	}

}
