/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.docvalues.impl;

import java.io.IOException;
import java.util.Objects;
import java.util.function.Function;

import org.hibernate.search.backend.lucene.lowlevel.join.impl.JoinChildrenIdIterator;
import org.hibernate.search.backend.lucene.lowlevel.join.impl.NestedDocsProvider;

import org.apache.lucene.index.DocValues;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.SortedNumericDocValues;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.util.BitSet;

/**
 * A source of {@link DoubleMultiValues} that automatically fetches values from nested documents if necessary.
 */
public abstract class JoiningDoubleMultiValuesSource extends DoubleMultiValuesSource {

	/**
	 * Creates a {@link JoiningDoubleMultiValuesSource} that wraps a double-valued field
	 *
	 * @param field the field
	 * @param nested the nested provider
	 * @return A {@link JoiningDoubleMultiValuesSource}
	 */
	public static JoiningDoubleMultiValuesSource fromDoubleField(String field, NestedDocsProvider nested) {
		return fromField( field, nested, SortedNumericDoubleDocValues::fromDoubleField );
	}

	/**
	 * Creates a {@link JoiningDoubleMultiValuesSource} that wraps a float-valued field
	 *
	 * @param field the field
	 * @param nested the nested provider
	 * @return A {@link JoiningDoubleMultiValuesSource}
	 */
	public static JoiningDoubleMultiValuesSource fromFloatField(String field, NestedDocsProvider nested) {
		return fromField( field, nested, SortedNumericDoubleDocValues::fromFloatField );
	}

	private static JoiningDoubleMultiValuesSource fromField(String field, NestedDocsProvider nested,
			Function<SortedNumericDocValues, SortedNumericDoubleDocValues> decoder) {
		return new FieldDoubleMultiValuesSource( field, nested, decoder );
	}

	protected final NestedDocsProvider nestedDocsProvider;

	public JoiningDoubleMultiValuesSource(NestedDocsProvider nestedDocsProvider) {
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
		JoiningDoubleMultiValuesSource that = (JoiningDoubleMultiValuesSource) o;
		return Objects.equals( nestedDocsProvider, that.nestedDocsProvider );
	}

	@Override
	public int hashCode() {
		return nestedDocsProvider.hashCode();
	}

	@Override
	public DoubleMultiValues getValues(LeafReaderContext ctx) throws IOException {
		SortedNumericDoubleDocValues values = getSortedNumericDoubleDocValues( ctx );

		if ( nestedDocsProvider == null ) {
			return DoubleMultiValues.fromDocValues( values );
		}

		final BitSet rootDocs = nestedDocsProvider.parentDocs( ctx );
		final DocIdSetIterator innerDocs = nestedDocsProvider.childDocs( ctx );
		return join( values, rootDocs, innerDocs );
	}

	protected abstract SortedNumericDoubleDocValues getSortedNumericDoubleDocValues(LeafReaderContext ctx) throws IOException;

	protected DoubleMultiValues join(SortedNumericDoubleDocValues values, final BitSet parentDocs,
			final DocIdSetIterator childDocs) {
		if ( parentDocs == null || childDocs == null ) {
			return DoubleMultiValues.EMPTY;
		}

		JoinChildrenIdIterator joinIterator = new JoinChildrenIdIterator( parentDocs, childDocs, values );

		return new DoubleMultiValues() {
			int currentParentDoc = -1;
			int remainingValuesForChild = 0;

			@Override
			public boolean advanceExact(int parentDoc) throws IOException {
				assert parentDoc >= currentParentDoc : "can only evaluate current and upcoming parent docs";
				if ( parentDoc == currentParentDoc ) {
					return hasNextValue();
				}

				currentParentDoc = parentDoc;
				remainingValuesForChild = 0; // To be set in the next call to hasNextValue()

				return joinIterator.advanceExact( parentDoc );
			}

			@Override
			public boolean hasNextValue() throws IOException {
				if ( remainingValuesForChild > 0 ) {
					return true;
				}

				if ( joinIterator.advanceValuesToNextChild() ) {
					remainingValuesForChild = values.docValueCount();
					return true;
				}
				else {
					remainingValuesForChild = 0;
					return false;
				}
			}

			@Override
			public double nextValue() throws IOException {
				--remainingValuesForChild;
				return values.nextValue();
			}
		};
	}

	private static class FieldDoubleMultiValuesSource extends JoiningDoubleMultiValuesSource {

		private final String field;
		private final Function<SortedNumericDocValues, SortedNumericDoubleDocValues> decoder;

		public FieldDoubleMultiValuesSource(String field, NestedDocsProvider nestedDocsProvider,
				Function<SortedNumericDocValues, SortedNumericDoubleDocValues> decoder) {
			super( nestedDocsProvider );
			this.field = field;
			this.decoder = decoder;
		}

		@Override
		public String toString() {
			return "double(" + field + "," + nestedDocsProvider + ")";
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( !super.equals( o ) ) {
				return false;
			}
			FieldDoubleMultiValuesSource that = (FieldDoubleMultiValuesSource) o;
			return Objects.equals( field, that.field )
					&& Objects.equals( decoder, that.decoder );
		}

		@Override
		public int hashCode() {
			return Objects.hash( super.hashCode(), field, decoder );
		}

		@Override
		protected SortedNumericDoubleDocValues getSortedNumericDoubleDocValues(LeafReaderContext ctx) throws IOException {
			// Numeric doc values are longs, but we want doubles
			return decoder.apply( DocValues.getSortedNumeric( ctx.reader(), field ) );
		}
	}

}
