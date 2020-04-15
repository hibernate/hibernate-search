/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.docvalues.impl;

import java.io.IOException;
import java.util.Objects;

import org.hibernate.search.backend.lucene.lowlevel.join.impl.JoinChildrenIdIterator;
import org.hibernate.search.backend.lucene.lowlevel.join.impl.NestedDocsProvider;

import org.apache.lucene.index.DocValues;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.SortedSetDocValues;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.util.BitSet;

/**
 * A source of {@link TextMultiValues} that automatically fetches values from nested documents if necessary.
 */
public abstract class JoiningTextMultiValuesSource extends TextMultiValuesSource {

	/**
	 * Creates a {@link JoiningTextMultiValuesSource} that wraps a text-valued field
	 *
	 * @param field the field
	 * @param nested the nested provider
	 * @return A {@link JoiningTextMultiValuesSource}
	 */
	public static JoiningTextMultiValuesSource fromField(String field, NestedDocsProvider nested) {
		return new FieldTextMultiValuesSource( field, nested );
	}

	protected final NestedDocsProvider nestedDocsProvider;

	public JoiningTextMultiValuesSource(NestedDocsProvider nestedDocsProvider) {
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
		JoiningTextMultiValuesSource that = (JoiningTextMultiValuesSource) o;
		return Objects.equals( nestedDocsProvider, that.nestedDocsProvider );
	}

	@Override
	public int hashCode() {
		return nestedDocsProvider.hashCode();
	}

	@Override
	public TextMultiValues getValues(LeafReaderContext ctx) throws IOException {
		SortedSetDocValues values = getSortedSetDocValues( ctx );

		if ( nestedDocsProvider == null ) {
			return TextMultiValues.fromDocValues( values );
		}

		final BitSet rootDocs = nestedDocsProvider.parentDocs( ctx );
		final DocIdSetIterator innerDocs = nestedDocsProvider.childDocs( ctx );
		return join( values, rootDocs, innerDocs );
	}

	protected abstract SortedSetDocValues getSortedSetDocValues(LeafReaderContext ctx) throws IOException;

	protected TextMultiValues join(SortedSetDocValues values, final BitSet parentDocs,
			final DocIdSetIterator childDocs) {
		if ( parentDocs == null || childDocs == null ) {
			return TextMultiValues.EMPTY;
		}

		JoinChildrenIdIterator joinIterator = new JoinChildrenIdIterator( parentDocs, childDocs, values );

		return new TextMultiValues.DocValuesTextMultiValues( values ) {
			int currentParentDoc = -1;

			@Override
			public boolean advanceExact(int parentDoc) throws IOException {
				assert parentDoc >= currentParentDoc : "can only evaluate current and upcoming parent docs";
				if ( parentDoc == currentParentDoc ) {
					return hasNextValue();
				}

				currentParentDoc = parentDoc;
				nextOrd = SortedSetDocValues.NO_MORE_ORDS; // To be set in the next call to hasNextValue()

				return joinIterator.advanceExact( parentDoc );
			}

			@Override
			public boolean hasNextValue() throws IOException {
				if ( nextOrd != SortedSetDocValues.NO_MORE_ORDS ) {
					return true;
				}

				if ( joinIterator.advanceValuesToNextChild() ) {
					nextOrd = values.nextOrd();
					return true;
				}
				else {
					nextOrd = SortedSetDocValues.NO_MORE_ORDS;
					return false;
				}
			}
		};
	}

	private static class FieldTextMultiValuesSource extends
			JoiningTextMultiValuesSource {

		private final String field;

		public FieldTextMultiValuesSource(String field, NestedDocsProvider nestedDocsProvider) {
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
			FieldTextMultiValuesSource that = (FieldTextMultiValuesSource) o;
			return Objects.equals( field, that.field );
		}

		@Override
		public int hashCode() {
			return Objects.hash( super.hashCode(), field );
		}

		@Override
		protected SortedSetDocValues getSortedSetDocValues(LeafReaderContext ctx) throws IOException {
			return DocValues.getSortedSet( ctx.reader(), field );
		}
	}

}
