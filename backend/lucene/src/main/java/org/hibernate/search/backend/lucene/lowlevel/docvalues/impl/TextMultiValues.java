/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.docvalues.impl;

import java.io.IOException;

import org.apache.lucene.index.SortedSetDocValues;
import org.apache.lucene.util.BytesRef;

/**
 * A per-document, unordered sequence of long values.
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


	/** Retrieves the value for the specified ordinal. The returned
	 * {@link BytesRef} may be re-used across calls to lookupOrd so make sure to
	 * {@link BytesRef#deepCopyOf(BytesRef) copy it} if you want to keep it
	 * around.
	 * @param ord ordinal to lookup
	 * @see #nextOrd
	 */
	public abstract BytesRef lookupOrd(long ord) throws IOException;

	/**
	 * Returns the number of unique values.
	 * @return number of unique values in this SortedDocValues. This is
	 *         also equivalent to one plus the maximum ordinal.
	 */
	public abstract long getValueCount();

	/** If {@code key} exists, returns its ordinal, else
	 *  returns {@code -insertionPoint-1}, like {@code
	 *  Arrays.binarySearch}.
	 *
	 *  @param key Key to look up
	 **/
	public abstract long lookupTerm(BytesRef key) throws IOException;

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
		public BytesRef lookupOrd(long ord) throws IOException {
			throw new IllegalArgumentException( "Ord " + ord + " is too large" );
		}

		@Override
		public long getValueCount() {
			return 0;
		}

		@Override
		public long lookupTerm(BytesRef key) throws IOException {
			return -1;
		}
	};

	protected static class DocValuesTextMultiValues extends TextMultiValues {

		protected final SortedSetDocValues values;
		protected long nextOrd;

		DocValuesTextMultiValues(SortedSetDocValues values) {
			this.values = values;
		}

		@Override
		public boolean advanceExact(int doc) throws IOException {
			boolean found = values.advanceExact( doc );
			nextOrd = found ? values.nextOrd() : SortedSetDocValues.NO_MORE_ORDS;
			return found;
		}

		@Override
		public boolean hasNextValue() throws IOException {
			return nextOrd != SortedSetDocValues.NO_MORE_ORDS;
		}

		@Override
		public long nextOrd() throws IOException {
			long result = nextOrd;
			nextOrd = values.nextOrd();
			return result;
		}

		@Override
		public BytesRef lookupOrd(long ord) throws IOException {
			return values.lookupOrd( ord );
		}

		@Override
		public long getValueCount() {
			return values.getValueCount();
		}

		@Override
		public long lookupTerm(BytesRef key) throws IOException {
			return values.lookupTerm( key );
		}
	}
}
