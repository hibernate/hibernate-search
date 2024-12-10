/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.lowlevel.docvalues.impl;

import java.io.IOException;
import java.util.Objects;
import java.util.function.Function;

import org.hibernate.search.backend.lucene.lowlevel.join.impl.ChildDocIds;
import org.hibernate.search.backend.lucene.lowlevel.join.impl.NestedDocsProvider;

import org.apache.lucene.index.DocValues;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.SortedNumericDocValues;
import org.apache.lucene.search.DoubleValues;
import org.apache.lucene.search.DoubleValuesSource;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.IndexSearcher;

/**
 * An implementation of {@link DoubleValuesSource} for docvalues with multiple values per document,
 * where multiple values are "aggregated" into a single value
 * according to a given {@link MultiValueMode}.
 * <p>
 * Some of this code was copied and adapted from
 * {@code org.elasticsearch.search.MultiValueMode}
 * from the <a href="https://github.com/elastic/elasticsearch">Elasticsearch project</a>.
 */
public abstract class DoubleMultiValuesToSingleValuesSource extends DoubleValuesSource {

	/**
	 * Creates a {@link DoubleMultiValuesToSingleValuesSource} that wraps a double-valued field
	 *
	 * @param field the field
	 * @param mode the mode
	 * @param nested the nested provider
	 * @return A {@link DoubleMultiValuesToSingleValuesSource}
	 */
	public static DoubleMultiValuesToSingleValuesSource fromDoubleField(String field, MultiValueMode mode,
			NestedDocsProvider nested) {
		return fromField( field, mode, nested, SortedNumericDoubleDocValues::fromDoubleField );
	}

	/**
	 * Creates a {@link DoubleMultiValuesToSingleValuesSource} that wraps a float-valued field
	 *
	 * @param field the field
	 * @param mode the mode
	 * @param nested the nested provider
	 * @return A {@link DoubleMultiValuesToSingleValuesSource}
	 */
	public static DoubleMultiValuesToSingleValuesSource fromFloatField(String field, MultiValueMode mode,
			NestedDocsProvider nested) {
		return fromField( field, mode, nested, SortedNumericDoubleDocValues::fromFloatField );
	}

	private static DoubleMultiValuesToSingleValuesSource fromField(String field, MultiValueMode mode, NestedDocsProvider nested,
			Function<SortedNumericDocValues, SortedNumericDoubleDocValues> decoder) {
		return new FieldMultiValuesToSingleValuesSource( field, mode, nested, decoder );
	}

	protected final MultiValueMode mode;
	protected final NestedDocsProvider nestedDocsProvider;

	public DoubleMultiValuesToSingleValuesSource(MultiValueMode mode, NestedDocsProvider nestedDocsProvider) {
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
		DoubleMultiValuesToSingleValuesSource that = (DoubleMultiValuesToSingleValuesSource) o;
		return Objects.equals( mode, that.mode )
				&& Objects.equals( nestedDocsProvider, that.nestedDocsProvider );
	}

	@Override
	public int hashCode() {
		return Objects.hash( mode, nestedDocsProvider );
	}

	@Override
	public NumericDoubleValues getValues(LeafReaderContext ctx, DoubleValues scores) throws IOException {
		SortedNumericDoubleDocValues values = getSortedNumericDoubleDocValues( ctx );

		if ( nestedDocsProvider == null ) {
			return select( values );
		}

		return select( values, nestedDocsProvider.childDocs( ctx, values ) );
	}

	protected abstract SortedNumericDoubleDocValues getSortedNumericDoubleDocValues(LeafReaderContext ctx) throws IOException;

	protected NumericDoubleValues select(SortedNumericDoubleDocValues values) {
		final NumericDoubleValues singleton = SortedNumericDoubleDocValues.unwrapSingleton( values );
		if ( singleton != null ) {
			return singleton;
		}
		else {
			return new NumericDoubleValues() {

				private double value;

				@Override
				public double doubleValue() {
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

	protected NumericDoubleValues select(SortedNumericDoubleDocValues values, ChildDocIds childDocsWithValues) {
		if ( childDocsWithValues == null ) {
			return NumericDoubleValues.EMPTY;
		}

		return new NumericDoubleValues() {

			int lastSeenParentDoc = -1;
			double lastEmittedValue = -1;

			@Override
			public double doubleValue() {
				return lastEmittedValue;
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
				lastEmittedValue = mode.pick( values, childDocsWithValues );
				return true;
			}

		};
	}

	private static class FieldMultiValuesToSingleValuesSource extends DoubleMultiValuesToSingleValuesSource {

		private final String field;
		private final Function<SortedNumericDocValues, SortedNumericDoubleDocValues> decoder;

		public FieldMultiValuesToSingleValuesSource(String field, MultiValueMode mode, NestedDocsProvider nestedDocsProvider,
				Function<SortedNumericDocValues, SortedNumericDoubleDocValues> decoder) {
			super( mode, nestedDocsProvider );
			this.field = field;
			this.decoder = decoder;
		}

		@Override
		public String toString() {
			return "double(" + field + "," + mode + "," + nestedDocsProvider + ")";
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
			return Objects.equals( field, that.field )
					&& Objects.equals( decoder, that.decoder );
		}

		@Override
		public int hashCode() {
			return Objects.hash( super.hashCode(), field, decoder );
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
		public Explanation explain(LeafReaderContext ctx, int docId, Explanation scoreExplanation) throws IOException {
			DoubleValues values = getValues( ctx, null );
			if ( values.advanceExact( docId ) ) {
				return Explanation.match( values.doubleValue(), this.toString() );
			}
			else {
				return Explanation.noMatch( this.toString() );
			}
		}

		@Override
		public DoubleValuesSource rewrite(IndexSearcher searcher) {
			return this;
		}

		@Override
		protected SortedNumericDoubleDocValues getSortedNumericDoubleDocValues(LeafReaderContext ctx) throws IOException {
			// Numeric doc values are longs, but we want doubles
			return decoder.apply( DocValues.getSortedNumeric( ctx.reader(), field ) );
		}
	}

}
