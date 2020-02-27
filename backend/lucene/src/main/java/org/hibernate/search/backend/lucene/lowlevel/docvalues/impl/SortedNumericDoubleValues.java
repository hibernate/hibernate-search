/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.docvalues.impl;

import java.io.IOException;
import java.util.function.LongToDoubleFunction;
import org.apache.lucene.index.DocValues;

import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.index.SortedNumericDocValues;
import org.apache.lucene.util.NumericUtils;

/**
 * Clone of {@link SortedNumericDocValues} for double values.
 * <p>
 * Copied and adapted from {@code org.elasticsearch.index.fielddata.SortedNumericDoubleValues} class
 * of <a href="https://github.com/elastic/elasticsearch">Elasticsearch project</a>.
 */
public abstract class SortedNumericDoubleValues {

	/**
	 * Sole constructor. (For invocation by subclass
	 * constructors, typically implicit.)
	 */
	protected SortedNumericDoubleValues() {
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

	public static SortedNumericDoubleValues create(NumericDocValues values, LongToDoubleFunction decoder) {
		return new SingletonSortedNumericDoubleValues( new SingleNumericDoubleValues( values, decoder ) );
	}

	public static SortedNumericDoubleValues createDouble(NumericDocValues values) {
		return create( values, NumericUtils::sortableLongToDouble );
	}

	public static SortedNumericDoubleValues createFloat(NumericDocValues values) {
		return create( values, (v) -> (double) NumericUtils.sortableIntToFloat( (int) v ) );
	}

	public static SortedNumericDoubleValues createLong(NumericDocValues values) {
		return create( values, (v) -> (double) v );
	}

	public static SortedNumericDoubleValues createInt(NumericDocValues values) {
		return createLong( values );
	}

	public static SortedNumericDoubleValues createDistance(GeoPointDistanceDocValues geoPointDistanceDocValues) {
		return new SingletonSortedNumericDoubleValues( geoPointDistanceDocValues );
	}

	public static SortedNumericDoubleValues castToDouble(final SortedNumericDocValues values) {
		final NumericDocValues singleton = DocValues.unwrapSingleton( values );
		if ( singleton != null ) {
			return singleton( new DoubleCastedValues( singleton ) );
		}
		else {
			return new SortedDoubleCastedValues( values );
		}
	}

	public static SortedNumericDoubleValues singleton(NumericDoubleValues values) {
		return new SingletonSortedNumericDoubleValues( values );
	}

	private static class SingleNumericDoubleValues extends NumericDoubleValues {

		private final NumericDocValues values;
		private final LongToDoubleFunction decoder;

		SingleNumericDoubleValues(NumericDocValues values, LongToDoubleFunction decoder) {
			this.values = values;
			this.decoder = decoder;
		}

		@Override
		public double doubleValue() throws IOException {
			return NumericUtils.sortableLongToDouble( values.longValue() );
		}

		@Override
		public boolean advanceExact(int doc) throws IOException {
			return values.advanceExact( doc );
		}

		public NumericDocValues getLongValues() {
			return values;
		}

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

}
