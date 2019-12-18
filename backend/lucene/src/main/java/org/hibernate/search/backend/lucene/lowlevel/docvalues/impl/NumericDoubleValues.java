/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.docvalues.impl;

import java.io.IOException;

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

	// TODO: this interaction with sort comparators is really ugly...

	/**
	 * Returns numeric docvalues view of raw double bits
	 */
	public NumericDocValues getRawDoubleValues() {
		return new NumericDocValues() {
			private int docID = -1;

			@Override
			public boolean advanceExact(int target) throws IOException {
				docID = target;
				return NumericDoubleValues.this.advanceExact( target );
			}

			@Override
			public long longValue() throws IOException {
				return Double.doubleToRawLongBits( NumericDoubleValues.this.doubleValue() );
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
		};
	}

	// yes... this is doing what the previous code was doing...

	/**
	 * Returns numeric docvalues view of raw float bits
	 */
	public NumericDocValues getRawFloatValues() {
		return new NumericDocValues() {
			private int docID = -1;

			@Override
			public boolean advanceExact(int target) throws IOException {
				docID = target;
				return NumericDoubleValues.this.advanceExact( target );
			}

			@Override
			public long longValue() throws IOException {
				return Float.floatToRawIntBits( (float) NumericDoubleValues.this.doubleValue() );
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
		};
	}
}
