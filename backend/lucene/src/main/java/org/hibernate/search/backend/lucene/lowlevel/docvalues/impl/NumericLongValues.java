/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.docvalues.impl;

import java.io.IOException;

import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.search.LongValues;

/**
 * A per-document numeric value.
 * <p>
 * Copied and adapted from {@code org.elasticsearch.index.fielddata.NumericDoubleValues} class
 * of <a href="https://github.com/elastic/elasticsearch">Elasticsearch project</a>.
 */
public abstract class NumericLongValues extends LongValues {

	/**
	 * Sole constructor. (For invocation by subclass
	 * constructors, typically implicit.)
	 */
	protected NumericLongValues() {
	}

	/**
	 * Returns numeric docvalues view of raw long bits
	 *
	 * @return numeric
	 */
	public NumericDocValues getRawLongValues() {
		return new RawNumericDocValues();
	}

	/**
	 * Retrieves the number of values for the current document.This must always
	 * be greater than zero. It is illegal to call this method after {@link #advanceExact(int)}
	 * returned {@code false}.
	 *
	 * @return value count
	 */
	public int docValueCount() {
		return 1;
	}

	public static final NumericLongValues LONG_VALUES_EMPTY = new NumericLongValues() {
		@Override
		public long longValue() {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean advanceExact(int doc) {
			return false;
		}

		@Override
		public int docValueCount() {
			return 0;
		}
	};

	/**
	 * Returns numeric docvalues view of raw int bits
	 *
	 * @return numeric
	 */
	public NumericDocValues getRawIntValues() {
		return getRawLongValues();
	}

	private class RawNumericDocValues extends NumericDocValues {
		private int docID = -1;

		public RawNumericDocValues() {
		}

		@Override
		public boolean advanceExact(int target) throws IOException {
			docID = target;
			return NumericLongValues.this.advanceExact( target );
		}

		@Override
		public long longValue() throws IOException {
			return NumericLongValues.this.longValue();
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
