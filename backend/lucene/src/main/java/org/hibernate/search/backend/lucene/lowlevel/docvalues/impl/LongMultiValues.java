/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.docvalues.impl;

import java.io.IOException;

import org.apache.lucene.index.SortedNumericDocValues;

/**
 * A per-document, unordered sequence of long values.
 */
public abstract class LongMultiValues {

	/**
	 * Sole constructor. (For invocation by subclass
	 * constructors, typically implicit.)
	 */
	protected LongMultiValues() {
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
	public abstract long nextValue() throws IOException;

	public static LongMultiValues fromDocValues(SortedNumericDocValues docValues) {
		return new DocValuesLongMultiValues( docValues );
	}

	/**
	 * An empty DoubleMultiValues instance that always returns {@code false} from {@link #advanceExact(int)}
	 */
	public static final LongMultiValues EMPTY = new LongMultiValues() {
		@Override
		public boolean advanceExact(int doc) {
			return false;
		}

		@Override
		public boolean hasNextValue() {
			throw new UnsupportedOperationException();
		}

		@Override
		public long nextValue() {
			throw new UnsupportedOperationException();
		}
	};

	protected static class DocValuesLongMultiValues extends LongMultiValues {

		private final SortedNumericDocValues values;
		private int remaining;

		DocValuesLongMultiValues(SortedNumericDocValues values) {
			this.values = values;
		}

		@Override
		public boolean advanceExact(int doc) throws IOException {
			boolean found = values.advanceExact( doc );
			updateRemaining( found );
			return found;
		}

		protected final void updateRemaining(boolean hasDocValue) {
			remaining = hasDocValue ? values.docValueCount() : 0;
		}

		@Override
		public boolean hasNextValue() throws IOException {
			return remaining > 0;
		}

		@Override
		public long nextValue() throws IOException {
			--remaining;
			return values.nextValue();
		}
	}
}
