/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.lowlevel.docvalues.impl;

import java.io.IOException;
import java.util.function.LongToDoubleFunction;

import org.apache.lucene.index.DocValues;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.index.SortedNumericDocValues;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.util.NumericUtils;

/**
 * Clone of {@link SortedNumericDocValues} for double values.
 * <p>
 * Copied and adapted from {@code org.elasticsearch.index.fielddata.SortedNumericDoubleValues} class
 * of <a href="https://github.com/elastic/elasticsearch">Elasticsearch project</a>.
 */
public abstract class SortedNumericDoubleDocValues extends DocIdSetIterator {

	/**
	 * Sole constructor. (For invocation by subclass
	 * constructors, typically implicit.)
	 */
	protected SortedNumericDoubleDocValues() {
	}

	/**
	 * Advance the iterator to exactly {@code target} and return whether
	 * {@code target} has a value.
	 * {@code target} must be greater than or equal to the current
	 * doc ID and must be a valid doc ID, ie. &ge; 0 and
	 * &lt; {@code maxDoc}.
	 *
	 * @param target the target
	 * @return the next value
	 * @throws java.io.IOException
	 */
	public abstract boolean advanceExact(int target) throws IOException;

	/**
	 * Iterates to the next value in the current document. Do not call this more than
	 * {@link #docValueCount} times for the document.
	 *
	 * @return next value
	 * @throws java.io.IOException
	 */
	public abstract double nextValue() throws IOException;

	/**
	 * Retrieves the number of values for the current document. This must always
	 * be greater than zero.
	 * It is illegal to call this method after {@link #advanceExact(int)}
	 * returned {@code false}.
	 *
	 * @return value count
	 */
	public abstract int docValueCount();

	public static SortedNumericDoubleDocValues fromDoubleField(SortedNumericDocValues values) {
		return create( values, NumericUtils::sortableLongToDouble );
	}

	public static SortedNumericDoubleDocValues fromFloatField(SortedNumericDocValues values) {
		return create( values, v -> (double) NumericUtils.sortableIntToFloat( (int) v ) );
	}

	public static NumericDoubleValues unwrapSingleton(SortedNumericDoubleDocValues values) {
		if ( values instanceof SingleValuedFieldNumericDoubleDocValues ) {
			return ( (SingleValuedFieldNumericDoubleDocValues) values ).toNumericDoubleValues();
		}
		else {
			return null;
		}
	}

	public static SortedNumericDoubleDocValues create(SortedNumericDocValues values, LongToDoubleFunction decoder) {
		NumericDocValues singleton = DocValues.unwrapSingleton( values );
		if ( singleton != null ) {
			return new SingleValuedFieldNumericDoubleDocValues( singleton, decoder );
		}
		return new MultiValuedFieldNumericDoubleDocValues( values, decoder );
	}

	private static class SingleValuedFieldNumericDoubleDocValues extends SingletonNumericDoubleDocValues {
		private final LongToDoubleFunction decoder;

		SingleValuedFieldNumericDoubleDocValues(NumericDocValues values, LongToDoubleFunction decoder) {
			super( values );
			this.decoder = decoder;
		}

		@Override
		public NumericDoubleValues toNumericDoubleValues() {
			return NumericDoubleValues.fromField( values, decoder );
		}

		@Override
		public double nextValue() throws IOException {
			return decoder.applyAsDouble( values.longValue() );
		}
	}

	private static class MultiValuedFieldNumericDoubleDocValues extends SortedNumericDoubleDocValues {
		private final SortedNumericDocValues values;
		private final LongToDoubleFunction decoder;

		MultiValuedFieldNumericDoubleDocValues(SortedNumericDocValues values, LongToDoubleFunction decoder) {
			this.values = values;
			this.decoder = decoder;
		}

		@Override
		public double nextValue() throws IOException {
			return decoder.applyAsDouble( values.nextValue() );
		}

		@Override
		public boolean advanceExact(int doc) throws IOException {
			return values.advanceExact( doc );
		}

		@Override
		public int advance(int target) throws IOException {
			return values.advance( target );
		}

		@Override
		public int nextDoc() throws IOException {
			return values.nextDoc();
		}

		@Override
		public int docID() {
			return values.docID();
		}

		@Override
		public int docValueCount() {
			return values.docValueCount();
		}

		@Override
		public long cost() {
			return values.cost();
		}
	}

}
