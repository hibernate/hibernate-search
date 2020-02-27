/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.docvalues.impl;

import java.io.IOException;
import org.apache.lucene.index.DocValues;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.index.SortedNumericDocValues;

/**
 * Utility methods, similar to Lucene's {@link DocValues}.
 */
public class FieldData {

	private FieldData() {
	}

	/**
	 * Return a {@link NumericDoubleValues} that doesn't contain any value.
	 *
	 * @return
	 */
	public static NumericDoubleValues emptyNumericDouble() {
		return new NumericDoubleValues() {
			@Override
			public boolean advanceExact(int doc) throws IOException {
				return false;
			}

			@Override
			public double doubleValue() throws IOException {
				throw new UnsupportedOperationException();
			}

		};
	}

	/**
	 * Return a {@link SortedNumericDoubleValues} that doesn't contain any
	 * value.
	 *
	 * @return
	 */
	public static SortedNumericDoubleValues emptySortedNumericDoubles() {
		return singleton( emptyNumericDouble() );
	}

	/**
	 * Returns a multi-valued view over the provided
	 * {@link NumericDoubleValues}.
	 *
	 * @param values
	 * @return
	 */
	public static SortedNumericDoubleValues singleton(NumericDoubleValues values) {
		return new SingletonSortedNumericDoubleValues( values );
	}

	/**
	 * Returns a single-valued view of the
	 * {@link SortedNumericDoubleValues}, if it was previously wrapped with
	 * {@link DocValues#singleton(NumericDocValues)}, or null.
	 *
	 * @param values
	 * @return
	 */
	public static NumericDoubleValues unwrapSingleton(SortedNumericDoubleValues values) {
		if ( values instanceof SingletonSortedNumericDoubleValues ) {
			return ((SingletonSortedNumericDoubleValues) values).getNumericDoubleValues();
		}
		return null;
	}

	/**
	 * Wrap the provided {@link SortedNumericDocValues} instance to cast all
	 * values to doubles.
	 *
	 * @param values
	 * @return
	 */
	public static SortedNumericDoubleValues castToDouble(final SortedNumericDocValues values) {
		final NumericDocValues singleton = DocValues.unwrapSingleton( values );
		if ( singleton != null ) {
			return singleton( new DoubleCastedValues( singleton ) );
		}
		else {
			return new SortedDoubleCastedValues( values );
		}
	}

	/**
	 * Wrap the provided {@link SortedNumericDocValues} instance to cast all
	 * values to floats.
	 *
	 * @param values
	 * @return
	 */
	public static SortedNumericDoubleValues castToFloat(final SortedNumericDocValues values) {
		final NumericDocValues singleton = DocValues.unwrapSingleton( values );
		if ( singleton != null ) {
			return singleton( new FloatCastedValues( singleton ) );
		}
		else {
			return new FloatDoubleCastedValues( values );
		}
	}

	/**
	 * Wrap the provided {@link SortedNumericDoubleValues} instance to cast
	 * all values to longs.
	 *
	 * @param values
	 * @return
	 */
	public static SortedNumericDocValues castToLong(final SortedNumericDoubleValues values) {
		final NumericDoubleValues singleton = unwrapSingleton( values );
		if ( singleton != null ) {
			return DocValues.singleton( new LongCastedValues( singleton ) );
		}
		else {
			return new SortedLongCastedValues( values );
		}
	}

	/**
	 * Return a {@link NumericDocValues} instance that has a value for every
	 * document, returns the same value as {@code values} if there is a
	 * value for the current document and {@code missing} otherwise.
	 *
	 * @param values
	 * @param missing
	 * @return
	 */
	public static NumericDocValues replaceMissing(NumericDocValues values, long missing) {
		return new AbstractNumericDocValues() {

			private long value;

			@Override
			public int docID() {
				return values.docID();
			}

			@Override
			public boolean advanceExact(int target) throws IOException {
				if ( values.advanceExact( target ) ) {
					value = values.longValue();
				}
				else {
					value = missing;
				}
				return true;
			}

			@Override
			public long longValue() throws IOException {
				return value;
			}
		};
	}

	/**
	 * Return a {@link NumericDoubleValues} instance that has a value for
	 * every document, returns the same value as {@code values} if there is
	 * a value for the current document and {@code missing} otherwise.
	 *
	 * @param values
	 * @param missing
	 * @return
	 */
	public static NumericDoubleValues replaceMissing(NumericDoubleValues values, double missing) {
		return new NumericDoubleValues() {

			private double value;

			@Override
			public boolean advanceExact(int target) throws IOException {
				if ( values.advanceExact( target ) ) {
					value = values.doubleValue();
				}
				else {
					value = missing;
				}
				return true;
			}

			@Override
			public double doubleValue() throws IOException {
				return value;
			}
		};
	}

	private static class DoubleCastedValues extends NumericDoubleValues {

		private final NumericDocValues values;

		DoubleCastedValues(NumericDocValues values) {
			this.values = values;
		}

		@Override
		public double doubleValue() throws IOException {
			return values.longValue();
		}

		@Override
		public boolean advanceExact(int doc) throws IOException {
			return values.advanceExact( doc );
		}

	}

	private static class SortedDoubleCastedValues extends SortedNumericDoubleValues {

		private final SortedNumericDocValues values;

		SortedDoubleCastedValues(SortedNumericDocValues in) {
			this.values = in;
		}

		@Override
		public boolean advanceExact(int target) throws IOException {
			return values.advanceExact( target );
		}

		@Override
		public double nextValue() throws IOException {
			return values.nextValue();
		}

		@Override
		public int docValueCount() {
			return values.docValueCount();
		}

	}

	private static class FloatCastedValues extends NumericDoubleValues {

		private final NumericDocValues values;

		FloatCastedValues(NumericDocValues values) {
			this.values = values;
		}

		@Override
		public double doubleValue() throws IOException {
			return values.longValue();
		}

		@Override
		public boolean advanceExact(int doc) throws IOException {
			return values.advanceExact( doc );
		}

	}

	private static class FloatDoubleCastedValues extends SortedNumericDoubleValues {

		private final SortedNumericDocValues values;

		FloatDoubleCastedValues(SortedNumericDocValues in) {
			this.values = in;
		}

		@Override
		public boolean advanceExact(int target) throws IOException {
			return values.advanceExact( target );
		}

		@Override
		public double nextValue() throws IOException {
			return values.nextValue();
		}

		@Override
		public int docValueCount() {
			return values.docValueCount();
		}

	}

	private static class LongCastedValues extends AbstractNumericDocValues {

		private final NumericDoubleValues values;
		private int docID = -1;

		LongCastedValues(NumericDoubleValues values) {
			this.values = values;
		}

		@Override
		public boolean advanceExact(int target) throws IOException {
			docID = target;
			return values.advanceExact( target );
		}

		@Override
		public long longValue() throws IOException {
			return (long) values.doubleValue();
		}

		@Override
		public int docID() {
			return docID;
		}
	}

	private static class SortedLongCastedValues extends AbstractSortedNumericDocValues {

		private final SortedNumericDoubleValues values;

		SortedLongCastedValues(SortedNumericDoubleValues in) {
			this.values = in;
		}

		@Override
		public boolean advanceExact(int target) throws IOException {
			return values.advanceExact( target );
		}

		@Override
		public int docValueCount() {
			return values.docValueCount();
		}

		@Override
		public long nextValue() throws IOException {
			return (long) values.nextValue();
		}

	}

}
