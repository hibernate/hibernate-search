/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.docvalues.impl;

import java.io.IOException;
import java.util.Objects;

import org.hibernate.search.backend.lucene.lowlevel.join.impl.ChildDocIds;
import org.hibernate.search.backend.lucene.lowlevel.join.impl.NestedDocsProvider;

import org.apache.lucene.index.DocValues;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.SortedNumericDocValues;
import org.apache.lucene.search.DocIdSetIterator;

/**
 * A source of {@link LongMultiValues} that automatically fetches values from nested documents if necessary.
 */
public abstract class JoiningLongMultiValuesSource extends LongMultiValuesSource {

	/**
	 * Creates a {@link JoiningLongMultiValuesSource} that wraps a Long-valued field
	 *
	 * @param field the field
	 * @param nested the nested provider
	 * @return A {@link JoiningLongMultiValuesSource}
	 */
	public static JoiningLongMultiValuesSource fromLongField(String field, NestedDocsProvider nested) {
		return fromField( field, nested );
	}

	/**
	 * Creates a {@link JoiningLongMultiValuesSource} that wraps an Integer-valued field
	 *
	 * @param field the field
	 * @param nested the nested provider
	 * @return A {@link JoiningLongMultiValuesSource}
	 */
	public static JoiningLongMultiValuesSource fromIntField(String field, NestedDocsProvider nested) {
		return fromField( field, nested );
	}

	private static JoiningLongMultiValuesSource fromField(String field, NestedDocsProvider nested) {
		return new FieldLongMultiValuesSource( field, nested );
	}

	protected final NestedDocsProvider nestedDocsProvider;

	public JoiningLongMultiValuesSource(NestedDocsProvider nestedDocsProvider) {
		this.nestedDocsProvider = nestedDocsProvider;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		JoiningLongMultiValuesSource that = (JoiningLongMultiValuesSource) o;
		return Objects.equals( nestedDocsProvider, that.nestedDocsProvider );
	}

	@Override
	public int hashCode() {
		return nestedDocsProvider.hashCode();
	}

	@Override
	public LongMultiValues getValues(LeafReaderContext ctx) throws IOException {
		SortedNumericDocValues values = getSortedNumericDocValues( ctx );

		if ( nestedDocsProvider == null ) {
			return LongMultiValues.fromDocValues( values );
		}

		return select( values, nestedDocsProvider.childDocs( ctx, values ) );
	}

	protected abstract SortedNumericDocValues getSortedNumericDocValues(LeafReaderContext ctx) throws IOException;

	protected LongMultiValues select(SortedNumericDocValues values, ChildDocIds childDocsWithValues) {
		if ( childDocsWithValues == null ) {
			return LongMultiValues.EMPTY;
		}

		return new LongMultiValues.DocValuesLongMultiValues( values ) {
			private int currentParentDoc = -1;

			@Override
			public boolean advanceExact(int parentDoc) throws IOException {
				assert parentDoc >= currentParentDoc : "can only evaluate current and upcoming parent docs";
				if ( parentDoc == currentParentDoc ) {
					return hasNextValue();
				}
				currentParentDoc = parentDoc;
				boolean found = childDocsWithValues.advanceExactParent( parentDoc );
				if ( found ) {
					// Position the iterator on the next child so that updateRemaining()
					// can get the relevant docvalues.
					childDocsWithValues.nextChild();
				}
				updateRemaining( found );
				return found;
			}

			@Override
			public boolean hasNextValue() throws IOException {
				if ( super.hasNextValue() ) {
					return true;
				}

				boolean hasNextChildDocWithValue = childDocsWithValues.nextChild() != DocIdSetIterator.NO_MORE_DOCS;
				updateRemaining( hasNextChildDocWithValue );
				return hasNextChildDocWithValue;
			}
		};
	}

	private static class FieldLongMultiValuesSource extends JoiningLongMultiValuesSource {

		private final String field;

		public FieldLongMultiValuesSource(String field, NestedDocsProvider nestedDocsProvider) {
			super( nestedDocsProvider );
			this.field = field;
		}

		@Override
		public String toString() {
			return "Long(" + field + "," + nestedDocsProvider + ")";
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( !super.equals( o ) ) {
				return false;
			}
			FieldLongMultiValuesSource that = (FieldLongMultiValuesSource) o;
			return Objects.equals( field, that.field );
		}

		@Override
		public int hashCode() {
			return Objects.hash( super.hashCode(), field );
		}

		@Override
		protected SortedNumericDocValues getSortedNumericDocValues(LeafReaderContext ctx) throws IOException {
			// Numeric doc values are longs, but we want Longs
			return DocValues.getSortedNumeric( ctx.reader(), field );
		}
	}

}
