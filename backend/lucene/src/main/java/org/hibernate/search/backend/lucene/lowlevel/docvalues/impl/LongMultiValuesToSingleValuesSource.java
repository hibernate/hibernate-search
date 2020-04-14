/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.docvalues.impl;

import java.io.IOException;
import java.util.Objects;
import org.apache.lucene.index.DocValues;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.index.SortedNumericDocValues;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.DoubleValues;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.LongValues;
import org.apache.lucene.search.LongValuesSource;
import org.apache.lucene.util.BitSet;

import org.hibernate.search.backend.lucene.lowlevel.join.impl.JoinFirstChildIdIterator;
import org.hibernate.search.backend.lucene.lowlevel.join.impl.NestedDocsProvider;

/**
 * An implementation of {@link LongValuesSource} for docvalues with multiple values per document,
 * where multiple values are "aggregated" into a single value
 * according to a given {@link MultiValueMode}.
 * <p>
 * Some of this code was copied and adapted from
 * {@code org.elasticsearch.search.MultiValueMode}
 * from the <a href="https://github.com/elastic/elasticsearch">Elasticsearch project</a>.
 */
public abstract class LongMultiValuesToSingleValuesSource extends LongValuesSource {

	/**
	 * Creates a {@link LongMultiValuesToSingleValuesSource} that wraps a long-valued field
	 *
	 * @param field the field
	 * @param mode the mode
	 * @param nested the nested provider
	 * @return A {@link LongMultiValuesToSingleValuesSource}
	 */
	public static LongMultiValuesToSingleValuesSource fromLongField(String field, MultiValueMode mode, NestedDocsProvider nested) {
		return fromField( field, mode, nested );
	}

	/**
	 * Creates a {@link LongMultiValuesToSingleValuesSource} that wraps an int-valued field
	 *
	 * @param field the field
	 * @param mode the mode
	 * @param nested the nested provider
	 * @return A {@link LongMultiValuesToSingleValuesSource}
	 */
	public static LongMultiValuesToSingleValuesSource fromIntField(String field, MultiValueMode mode, NestedDocsProvider nested) {
		return fromLongField( field, mode, nested );
	}

	private static LongMultiValuesToSingleValuesSource fromField(String field, MultiValueMode mode, NestedDocsProvider nested) {
		return new MultiFieldValuesToSingleValuesSource( field, mode, nested );
	}

	protected final MultiValueMode mode;
	protected final NestedDocsProvider nestedDocsProvider;

	public LongMultiValuesToSingleValuesSource(MultiValueMode mode, NestedDocsProvider nestedDocsProvider) {
		this.mode = mode;
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
		LongMultiValuesToSingleValuesSource that = (LongMultiValuesToSingleValuesSource) o;
		return Objects.equals( mode, that.mode )
				&& Objects.equals( nestedDocsProvider, that.nestedDocsProvider );
	}

	@Override
	public int hashCode() {
		return Objects.hash( mode, nestedDocsProvider );
	}

	@Override
	public LongValues getValues(LeafReaderContext ctx, DoubleValues scores) throws IOException {
		SortedNumericDocValues values = getSortedNumericDocValues( ctx );

		if ( nestedDocsProvider == null ) {
			return select( values );
		}

		final BitSet rootDocs = nestedDocsProvider.parentDocs( ctx );
		final DocIdSetIterator innerDocs = nestedDocsProvider.childDocs( ctx );
		return select( values, rootDocs, innerDocs );
	}

	/**
	 * Returns a {@link NumericDocValues} instance for the passed-in LeafReaderContext and scores
	 *
	 * If scores are not needed to calculate the values (ie {@link #needsScores() returns false}, callers
	 * may safely pass {@code null} for the {@code scores} parameter.
	 *
	 * @param ctx the ctx
	 * @param scores the scores
	 * @return NumericDocValues
	 * @throws java.io.IOException
	 */
	public NumericDocValues getRawNumericDocValues(LeafReaderContext ctx, DoubleValues scores) throws IOException {
		return new RawNumericDocValues( getValues( ctx, scores ) );
	}

	protected abstract SortedNumericDocValues getSortedNumericDocValues(LeafReaderContext ctx) throws IOException;

	protected LongValues select(final SortedNumericDocValues values) {
		final NumericDocValues singleton = DocValues.unwrapSingleton( values );
		if ( singleton != null ) {
			return new LongValues() {
				@Override
				public long longValue() throws IOException {
					return singleton.longValue();
				}

				@Override
				public boolean advanceExact(int target) throws IOException {
					return singleton.advanceExact( target );
				}
			};
		}
		else {
			return new LongValues() {
				private long value;

				@Override
				public long longValue() {
					return value;
				}

				@Override
				public boolean advanceExact(int doc) throws IOException {
					if ( values.advanceExact( doc ) ) {
						value = mode.pick( values );
						return true;
					}
					return false;
				}
			};
		}
	}

	protected LongValues select(final SortedNumericDocValues values, final BitSet parentDocs,
			final DocIdSetIterator childDocs) {
		if ( parentDocs == null || childDocs == null ) {
			return DocValuesUtils.LONG_VALUES_EMPTY;
		}

		JoinFirstChildIdIterator joinIterator = new JoinFirstChildIdIterator( parentDocs, childDocs, values );

		return new LongValues() {
			int lastSeenParentDoc = -1;
			long lastEmittedValue = -1;

			@Override
			public long longValue() {
				return lastEmittedValue;
			}

			@Override
			public boolean advanceExact(int parentDoc) throws IOException {
				assert parentDoc >= lastSeenParentDoc : "can only evaluate current and upcoming parent docs";
				if ( parentDoc == lastSeenParentDoc ) {
					return true;
				}

				int nextChildWithValue = joinIterator.advance( parentDoc );
				if ( nextChildWithValue == JoinFirstChildIdIterator.NO_CHILD_WITH_VALUE ) {
					// No child of this parent has a value
					return false;
				}

				lastSeenParentDoc = parentDoc;
				lastEmittedValue = mode.pick( values, childDocs, nextChildWithValue, parentDoc );
				return true;
			}
		};
	}

	private static class MultiFieldValuesToSingleValuesSource extends LongMultiValuesToSingleValuesSource {

		private final String field;

		private MultiFieldValuesToSingleValuesSource(String field, MultiValueMode mode, NestedDocsProvider nested) {
			super( mode, nested );
			this.field = field;
		}

		@Override
		public String toString() {
			return "long(" + field + "," + mode + "," + nestedDocsProvider + ")";
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( !super.equals( o ) ) {
				return false;
			}
			MultiFieldValuesToSingleValuesSource that = (MultiFieldValuesToSingleValuesSource) o;
			return Objects.equals( field, that.field );
		}

		@Override
		public int hashCode() {
			return Objects.hash( field );
		}

		@Override
		public boolean needsScores() {
			return false;
		}

		@Override
		public boolean isCacheable(LeafReaderContext ctx) {
			return DocValues.isCacheable( ctx, field );
		}

		@Override
		public LongValuesSource rewrite(IndexSearcher searcher) {
			return this;
		}

		@Override
		protected SortedNumericDocValues getSortedNumericDocValues(LeafReaderContext ctx) throws IOException {
			return DocValues.getSortedNumeric( ctx.reader(), field );
		}
	}

	private static class RawNumericDocValues extends NumericDocValues {
		private int docID = -1;
		private final LongValues values;

		public RawNumericDocValues(LongValues values) {
			this.values = values;
		}

		@Override
		public boolean advanceExact(int target) throws IOException {
			docID = target;
			return values.advanceExact( target );
		}

		@Override
		public long longValue() throws IOException {
			return values.longValue();
		}

		@Override
		public int docID() {
			return docID;
		}

		@Override
		public int nextDoc() {
			throw new UnsupportedOperationException();
		}

		@Override
		public int advance(int target) {
			throw new UnsupportedOperationException();
		}

		@Override
		public long cost() {
			throw new UnsupportedOperationException();
		}
	}

}
