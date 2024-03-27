/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.lowlevel.docvalues.impl;

import java.io.IOException;

import org.apache.lucene.index.SortedSetDocValues;

/**
 * A per-document, unordered sequence of text ordinals.
 * <p>
 * Essentially, this is a wrapper around {@link SortedSetDocValues} that may support joins,
 * but does not guarantee that ordinals for a given documents are returned in ascending order.
 * <p>
 * Some of this code was copied and adapted from
 * {@code org.apache.lucene.index.SortedSetDocValues}
 * of <a href="https://lucene.apache.org/">Apache Lucene project</a>.
 */
public abstract class TextMultiValues {

	/**
	 * Sole constructor. (For invocation by subclass
	 * constructors, typically implicit.)
	 */
	protected TextMultiValues() {
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
	public abstract long nextOrd() throws IOException;

	/**
	 * Returns the number of unique values.
	 * @return number of unique values in this SortedDocValues. This is
	 *         also equivalent to one plus the maximum ordinal.
	 */
	public abstract long getValueCount();

	public static TextMultiValues fromDocValues(SortedSetDocValues docValues) {
		return new DocValuesTextMultiValues( docValues );
	}

	/**
	 * An empty DoubleMultiValues instance that always returns {@code false} from {@link #advanceExact(int)}
	 */
	public static final TextMultiValues EMPTY = new TextMultiValues() {

		@Override
		public boolean advanceExact(int doc) {
			return false;
		}

		@Override
		public boolean hasNextValue() {
			throw new UnsupportedOperationException();
		}

		@Override
		public long nextOrd() {
			throw new UnsupportedOperationException();
		}

		@Override
		public long getValueCount() {
			return 0;
		}
	};

	protected static class DocValuesTextMultiValues extends TextMultiValues {
		protected final SortedSetDocValues values;
		private int remaining;

		DocValuesTextMultiValues(SortedSetDocValues values) {
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
		public long nextOrd() throws IOException {
			--remaining;
			return values.nextOrd();
		}

		@Override
		public long getValueCount() {
			return values.getValueCount();
		}
	}
}
