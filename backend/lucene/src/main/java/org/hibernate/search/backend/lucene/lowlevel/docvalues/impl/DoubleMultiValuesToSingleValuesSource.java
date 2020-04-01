/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.docvalues.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.DoubleToLongFunction;
import java.util.function.Function;

import org.hibernate.search.backend.lucene.lowlevel.join.impl.JoinFirstChildIdIterator;
import org.hibernate.search.backend.lucene.lowlevel.join.impl.NestedDocsProvider;

import org.apache.lucene.index.DocValues;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.index.SortedNumericDocValues;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.DoubleValues;
import org.apache.lucene.search.DoubleValuesSource;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.LongValues;
import org.apache.lucene.search.LongValuesSource;
import org.apache.lucene.util.BitSet;
import org.apache.lucene.util.NumericUtils;

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

	protected final DoubleToLongFunction encoder;

	/**
	 * Creates a {@link DoubleMultiValuesToSingleValuesSource} that wraps a double-valued field
	 *
	 * @param field the field
	 * @param mode the mode
	 * @param nested the nested provider
	 * @return A {@link DoubleMultiValuesToSingleValuesSource}
	 */
	public static DoubleMultiValuesToSingleValuesSource fromDoubleField(String field, MultiValueMode mode, NestedDocsProvider nested) {
		return fromField( field, mode, nested, SortedNumericDoubleDocValues::fromDoubleField, NumericUtils::doubleToSortableLong );
	}

	/**
	 * Creates a {@link DoubleMultiValuesToSingleValuesSource} that wraps a float-valued field
	 *
	 * @param field the field
	 * @param mode the mode
	 * @param nested the nested provider
	 * @return A {@link DoubleMultiValuesToSingleValuesSource}
	 */
	public static DoubleMultiValuesToSingleValuesSource fromFloatField(String field, MultiValueMode mode, NestedDocsProvider nested) {
		return fromField( field, mode, nested, SortedNumericDoubleDocValues::fromFloatField, (v) -> (long) NumericUtils.floatToSortableInt( (float) v ) );
	}

	private static DoubleMultiValuesToSingleValuesSource fromField(String field, MultiValueMode mode, NestedDocsProvider nested,
		Function<SortedNumericDocValues, SortedNumericDoubleDocValues> decoder, DoubleToLongFunction encoder) {
		return new FieldMultiValuesToSingleValuesSource( field, mode, nested, decoder, encoder );
	}

	protected final MultiValueMode mode;
	protected final NestedDocsProvider nestedDocsProvider;

	public DoubleMultiValuesToSingleValuesSource(MultiValueMode mode, NestedDocsProvider nestedDocsProvider, DoubleToLongFunction encoder) {
		this.mode = mode;
		this.nestedDocsProvider = nestedDocsProvider;
		this.encoder = encoder;
	}

	/**
	 * Convert to a LongValuesSource by casting the double values to longs
	 *
	 * @return LongValuesSource
	 */
	public LongMultiValuesToSingleValuesSource getLongValuesSource() {
		return new LongMultiDoubleValuesSource( this, mode, nestedDocsProvider, encoder );
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
			return replaceScores( select( values ), scores );
		}

		final BitSet rootDocs = nestedDocsProvider.parentDocs( ctx );
		final DocIdSetIterator innerDocs = nestedDocsProvider.childDocs( ctx );
		return replaceScores( select( values, rootDocs, innerDocs ), scores );
	}

	/**
	 * Convert to a LongValuesSource by encoding the double values to longs
	 * (using either {@link Double#doubleToRawLongBits(double)} or {@link Float#floatToRawIntBits(float)}).
	 *
	 * @param encoder
	 * @return LongValuesSource
	 */
	public LongValuesSource toRawValuesSource(Function<NumericDoubleValues, NumericDocValues> encoder) {
		return new LongDoubleValuesSource( this, encoder );
	}

	protected abstract SortedNumericDoubleDocValues getSortedNumericDoubleDocValues(LeafReaderContext ctx) throws IOException;

	protected NumericDoubleValues select(SortedNumericDoubleDocValues values) {
		final NumericDoubleValues singleton = SortedNumericDoubleDocValues.unwrapSingleton( values );
		if ( singleton != null ) {
			return singleton;
		}
		else if ( mode == null || mode == MultiValueMode.NONE ) {
			return new NumericDoubleValues() {

				@Override
				public double doubleValue() throws IOException {
					return values.nextValue();
				}

				@Override
				public int docValueCount() {
					return values.docValueCount();
				}

				@Override
				public boolean advanceExact(int doc) throws IOException {
					return values.advanceExact( doc );
				}
			};
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

	protected NumericDoubleValues select(SortedNumericDoubleDocValues values, final BitSet parentDocs,
		final DocIdSetIterator childDocs) {
		if ( parentDocs == null || childDocs == null ) {
			return NumericDoubleValues.EMPTY;
		}

		JoinFirstChildIdIterator joinIterator = new JoinFirstChildIdIterator( parentDocs, childDocs, values );

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

	protected List<Double> list(SortedNumericDoubleDocValues values, DocIdSetIterator docItr, int startDoc, int endDoc) throws IOException {
		List<Double> result = new ArrayList<>();
		int count = 0;
		for ( int doc = startDoc; doc < endDoc; doc = docItr.nextDoc() ) {
			if ( values.advanceExact( doc ) ) {
				int docValueCount = values.docValueCount();
				for ( int i = 0; i < docValueCount; i++ ) {
					result.add( values.nextValue() );
				}
			}
		}

		Collections.sort( result );
		return result;
	}

	protected NumericDoubleValues replaceScores(NumericDoubleValues values, DoubleValues scores) {
		return new NumericDoubleValues() {

			private double value;
			private int count;

			@Override
			public boolean advanceExact(int target) throws IOException {
				boolean result = false;
				if ( values.advanceExact( target ) ) {
					value = values.doubleValue();
					count = values.docValueCount();
					result = true;
				}
				else if ( scores != null && scores.advanceExact( target ) ) {
					value = scores.doubleValue();
					count = 1;
					result = true;
				}
				return result;
			}

			@Override
			public double doubleValue() throws IOException {
				return value;
			}

			@Override
			public int docValueCount() {
				return count;
			}
		};
	}

	private static class FieldMultiValuesToSingleValuesSource extends DoubleMultiValuesToSingleValuesSource {

		private final String field;
		private final Function<SortedNumericDocValues, SortedNumericDoubleDocValues> decoder;

		public FieldMultiValuesToSingleValuesSource(String field, MultiValueMode mode, NestedDocsProvider nestedDocsProvider,
			Function<SortedNumericDocValues, SortedNumericDoubleDocValues> decoder, DoubleToLongFunction encoder) {
			super( mode, nestedDocsProvider, encoder );
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

	private static class LongDoubleValuesSource extends LongValuesSource {

		private final DoubleMultiValuesToSingleValuesSource inner;
		private final Function<NumericDoubleValues, NumericDocValues> encoder;

		private LongDoubleValuesSource(DoubleMultiValuesToSingleValuesSource inner,
			Function<NumericDoubleValues, NumericDocValues> encoder) {
			this.inner = inner;
			this.encoder = encoder;
		}

		@Override
		public LongValues getValues(LeafReaderContext ctx, DoubleValues scores) throws IOException {
			NumericDoubleValues in = inner.getValues( ctx, scores );
			NumericDocValues rawValues = encoder.apply( in );
			return new LongValues() {
				@Override
				public long longValue() throws IOException {
					return rawValues.longValue();
				}

				@Override
				public boolean advanceExact(int doc) throws IOException {
					return rawValues.advanceExact( doc );
				}
			};
		}

		@Override
		public boolean isCacheable(LeafReaderContext ctx) {
			return inner.isCacheable( ctx );
		}

		@Override
		public boolean needsScores() {
			return inner.needsScores();
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			LongDoubleValuesSource that = (LongDoubleValuesSource) o;
			return Objects.equals( inner, that.inner );
		}

		@Override
		public int hashCode() {
			return Objects.hash( inner );
		}

		@Override
		public String toString() {
			return "long(" + inner.toString() + ")";
		}

		@Override
		public LongValuesSource rewrite(IndexSearcher searcher) throws IOException {
			return inner.rewrite( searcher ).toLongValuesSource();
		}

	}

	private static class LongMultiDoubleValuesSource extends LongMultiValuesToSingleValuesSource {
		private final DoubleMultiValuesToSingleValuesSource inner;
		private final DoubleToLongFunction encoder;

		public LongMultiDoubleValuesSource(DoubleMultiValuesToSingleValuesSource inner, MultiValueMode mode,
			NestedDocsProvider nestedDocsProvider, DoubleToLongFunction encoder) {
			super( mode, nestedDocsProvider );
			this.inner = inner;
			this.encoder = encoder;
		}

		@Override
		public NumericLongValues getValues(LeafReaderContext ctx, DoubleValues scores) throws IOException {
			NumericDoubleValues in = inner.getValues( ctx, scores );
			return new NumericLongValues() {
				@Override
				public long longValue() throws IOException {
					return encoder.applyAsLong( in.doubleValue() );
				}

				@Override
				public boolean advanceExact(int doc) throws IOException {
					return in.advanceExact( doc );
				}

				@Override
				public int docValueCount() {
					return in.docValueCount();
				}
			};
		}

		@Override
		public boolean isCacheable(LeafReaderContext ctx) {
			return inner.isCacheable( ctx );
		}

		@Override
		public boolean needsScores() {
			return inner.needsScores();
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			LongDoubleValuesSource that = (LongDoubleValuesSource) o;
			return Objects.equals( inner, that.inner );
		}

		@Override
		public int hashCode() {
			return Objects.hash( inner );
		}

		@Override
		public String toString() {
			return "long(" + inner.toString() + ")";
		}

		@Override
		public LongValuesSource rewrite(IndexSearcher searcher) throws IOException {
			return inner.rewrite( searcher ).toLongValuesSource();
		}

		@Override
		protected SortedNumericDocValues getSortedNumericDocValues(LeafReaderContext ctx) throws IOException {
			SortedNumericDoubleDocValues in = inner.getSortedNumericDoubleDocValues( ctx );

			return new SortedNumericDocValues() {
				@Override
				public long nextValue() throws IOException {
					return encoder.applyAsLong( in.nextValue() );
				}

				@Override
				public int docValueCount() {
					return in.docValueCount();
				}

				@Override
				public boolean advanceExact(int target) throws IOException {
					return in.advanceExact( target );
				}

				@Override
				public int docID() {
					return in.docID();
				}

				@Override
				public int nextDoc() throws IOException {
					return in.nextDoc();
				}

				@Override
				public int advance(int target) throws IOException {
					return in.advance( target );
				}

				@Override
				public long cost() {
					return in.cost();
				}
			};
		}
	}

}
