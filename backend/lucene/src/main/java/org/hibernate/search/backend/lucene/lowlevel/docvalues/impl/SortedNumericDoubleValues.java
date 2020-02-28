/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.docvalues.impl;

import java.io.IOException;

import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.index.SortedNumericDocValues;

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
	 */
	public abstract boolean advanceExact(int target) throws IOException;

	/**
	 * Iterates to the next value in the current document. Do not call this more than
	 * {@link #docValueCount} times for the document.
	 */
	public abstract double nextValue() throws IOException;

	/**
	 * Retrieves the number of values for the current document.  This must always
	 * be greater than zero.
	 * It is illegal to call this method after {@link #advanceExact(int)}
	 * returned {@code false}.
	 */
	public abstract int docValueCount();

	public static SortedNumericDoubleValues createDouble(NumericDocValues values) {
		return new SingletonSortedNumericDoubleValues( new SortableLongBitsToNumericDoubleValues( values ) );
	}

	public static SortedNumericDoubleValues createFloat(NumericDocValues values) {
		return new SingletonSortedNumericDoubleValues( new SingleFloatValues( values ) );
	}

	public static SortedNumericDoubleValues createDistance(GeoPointDistanceDocValues geoPointDistanceDocValues) {
		return new SingletonSortedNumericDoubleValues( geoPointDistanceDocValues );
	}
}
