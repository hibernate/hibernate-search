/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.lowlevel.docvalues.impl;

import java.io.IOException;
import java.util.Objects;

import org.hibernate.search.backend.lucene.lowlevel.join.impl.ChildDocIds;
import org.hibernate.search.backend.lucene.lowlevel.join.impl.NestedDocsProvider;

import org.apache.lucene.index.DocValues;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.SortedDocValues;
import org.apache.lucene.index.SortedSetDocValues;
import org.apache.lucene.util.BytesRef;

/**
 * A source of {@link org.apache.lucene.index.SortedDocValues} (text doc values) with multiple values per document,
 * where multiple values are "aggregated" into a single value
 * according to a given {@link MultiValueMode}.
 * <p>
 * Some of this code was copied and adapted from
 * {@code org.elasticsearch.search.MultiValueMode}
 * from the <a href="https://github.com/elastic/elasticsearch">Elasticsearch project</a>.
 */
public abstract class TextMultiValuesToSingleValuesSource {

	/**
	 * Creates a {@link TextMultiValuesToSingleValuesSource} that wraps a text field
	 *
	 * @param field the field
	 * @param mode the mode
	 * @param nested the nested provider
	 * @return DoubleMultiValuesSource
	 */
	public static TextMultiValuesToSingleValuesSource fromField(String field, MultiValueMode mode, NestedDocsProvider nested) {
		return new FieldMultiValuesToSingleValuesSource( field, mode, nested );
	}

	protected final MultiValueMode mode;
	protected final NestedDocsProvider nestedDocsProvider;

	public TextMultiValuesToSingleValuesSource(MultiValueMode mode, NestedDocsProvider nestedDocsProvider) {
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
		TextMultiValuesToSingleValuesSource that = (TextMultiValuesToSingleValuesSource) o;
		return Objects.equals( mode, that.mode )
				&& Objects.equals( nestedDocsProvider, that.nestedDocsProvider );
	}

	@Override
	public int hashCode() {
		return Objects.hash( mode, nestedDocsProvider );
	}

	public SortedDocValues getValues(LeafReaderContext ctx) throws IOException {
		SortedSetDocValues values = getSortedSetDocValues( ctx );

		if ( nestedDocsProvider == null ) {
			return select( values );
		}

		return select( values, nestedDocsProvider.childDocs( ctx, values ) );
	}

	protected abstract SortedSetDocValues getSortedSetDocValues(LeafReaderContext ctx) throws IOException;

	protected SortedDocValues select(SortedSetDocValues values) {
		final SortedDocValues singleton = DocValues.unwrapSingleton( values );
		if ( singleton != null ) {
			return singleton;
		}
		else {
			return new SortedSetDocValuesToSortedDocValuesWrapper( values ) {
				int docID = -1;
				int lastEmittedOrd = -1;

				@Override
				public int ordValue() {
					return lastEmittedOrd;
				}

				@Override
				public int docID() {
					return docID;
				}

				@Override
				public boolean advanceExact(int doc) throws IOException {
					if ( values.advanceExact( doc ) ) {
						lastEmittedOrd = (int) mode.pick( values );
						docID = doc;
						return true;
					}
					return false;
				}
			};
		}
	}

	protected SortedDocValues select(SortedSetDocValues values, ChildDocIds childDocsWithValues) {
		if ( childDocsWithValues == null ) {
			return DocValues.emptySorted();
		}

		return new SortedSetDocValuesToSortedDocValuesWrapper( values ) {
			int lastSeenParentDoc = -1;
			int lastEmittedOrd = -1;

			@Override
			public int ordValue() {
				return lastEmittedOrd;
			}

			@Override
			public int docID() {
				return lastSeenParentDoc;
			}

			@Override
			public boolean advanceExact(int parentDoc) throws IOException {
				assert parentDoc >= lastSeenParentDoc : "can only evaluate current and upcoming parent docs";
				if ( parentDoc == lastSeenParentDoc ) {
					return true;
				}

				if ( !childDocsWithValues.advanceExactParent( parentDoc ) ) {
					// No child of this parent has a value
					return false;
				}

				lastSeenParentDoc = parentDoc;
				lastEmittedOrd = (int) mode.pick( values, childDocsWithValues );
				return true;
			}
		};
	}

	private static class FieldMultiValuesToSingleValuesSource extends TextMultiValuesToSingleValuesSource {

		private final String field;

		public FieldMultiValuesToSingleValuesSource(String field, MultiValueMode mode, NestedDocsProvider nestedDocsProvider) {
			super( mode, nestedDocsProvider );
			this.field = field;
		}

		@Override
		public String toString() {
			return "text(" + field + "," + mode + "," + nestedDocsProvider + ")";
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( !super.equals( o ) ) {
				return false;
			}
			FieldMultiValuesToSingleValuesSource that = (FieldMultiValuesToSingleValuesSource) o;
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

	private abstract static class SortedSetDocValuesToSortedDocValuesWrapper extends SortedDocValues {

		private final SortedSetDocValues delegate;

		SortedSetDocValuesToSortedDocValuesWrapper(SortedSetDocValues delegate) {
			this.delegate = delegate;
			if ( delegate.getValueCount() > Integer.MAX_VALUE ) {
				// We may want to remove this limitation?
				// It would require defining our own FieldComparator mimicking TermOrdValComparator, which is pretty complex...
				// Note that single-valued text docvalues are limited to that many different terms anyway,
				// so this is no worse than the "legacy" sorts on single-valued text fields.
				throw new IllegalStateException( "Cannot sort when more than " + Integer.MAX_VALUE + " terms are indexed" );
			}
		}

		@Override
		public int getValueCount() {
			return (int) delegate.getValueCount();
		}

		@Override
		public BytesRef lookupOrd(int ord) throws IOException {
			return delegate.lookupOrd( ord );
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
