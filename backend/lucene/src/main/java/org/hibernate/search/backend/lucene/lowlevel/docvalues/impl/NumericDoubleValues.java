/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.lowlevel.docvalues.impl;

import java.io.IOException;
import java.util.function.DoubleToLongFunction;
import java.util.function.LongToDoubleFunction;

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
		return new RawNumericDocValues( v -> (long) Float.floatToRawIntBits( (float) v ) );
	}

	public static NumericDoubleValues fromField(NumericDocValues values, LongToDoubleFunction decoder) {
		return new FieldNumericDoubleValues( values, decoder );
	}

	/**
	 * An empty NumericDoubleValues instance that always returns {@code false} from {@link #advanceExact(int)}
	 */
	public static final NumericDoubleValues EMPTY = new NumericDoubleValues() {
		@Override
		public double doubleValue() {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean advanceExact(int doc) {
			return false;
		}
	};

	private static class FieldNumericDoubleValues extends NumericDoubleValues {

		private final NumericDocValues values;
		private final LongToDoubleFunction decoder;

		FieldNumericDoubleValues(NumericDocValues values, LongToDoubleFunction decoder) {
			this.values = values;
			this.decoder = decoder;
		}

		@Override
		public double doubleValue() throws IOException {
			return decoder.applyAsDouble( values.longValue() );
		}

		@Override
		public boolean advanceExact(int doc) throws IOException {
			return values.advanceExact( doc );
		}

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
