/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.docvalues.impl;

import java.io.IOException;

/**
 * A per-document, unordered sequence of double values.
 */
public abstract class DoubleMultiValues {

	protected DoubleMultiValues() {
	}

	/**
	 * Advance this instance to the given document id
	 *
	 * @return true if there is a value for this document
	 */
	public abstract boolean advanceExact(int doc) throws IOException;

	/**
	 * @return true if there is a next value for this document,
	 * i.e. if nextValue() can be called.
	 */
	public abstract boolean hasNextValue() throws IOException;

	/**
	 * @return The next value for the current document.
	 * Can only be called after {@link #hasNextValue()} returned {@code true}.
	 */
	public abstract double nextValue() throws IOException;

	public static DoubleMultiValues fromDocValues(SortedNumericDoubleDocValues docValues) {
		return new DocValuesDoubleMultiValues( docValues );
	}

	/**
	 * An empty DoubleMultiValues instance that always returns {@code false} from {@link #advanceExact(int)}
	 */
	public static final DoubleMultiValues EMPTY = new DoubleMultiValues() {
		@Override
		public boolean advanceExact(int doc) {
			return false;
		}

		@Override
		public boolean hasNextValue() {
			throw new UnsupportedOperationException();
		}

		@Override
		public double nextValue() {
			throw new UnsupportedOperationException();
		}
	};


	private static class DocValuesDoubleMultiValues extends DoubleMultiValues {

		private final SortedNumericDoubleDocValues values;
		private int remaining;

		DocValuesDoubleMultiValues(SortedNumericDoubleDocValues values) {
			this.values = values;
		}

		@Override
		public boolean advanceExact(int doc) throws IOException {
			boolean found = values.advanceExact( doc );
			this.remaining = found ? values.docValueCount() : 0;
			return found;
		}

		@Override
		public boolean hasNextValue() {
			return remaining > 0;
		}

		@Override
		public double nextValue() throws IOException {
			--remaining;
			return values.nextValue();
		}
	}
}
