/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.sort.impl;

import java.lang.invoke.MethodHandles;
import java.time.temporal.TemporalAccessor;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.search.common.impl.AbstractLuceneCodecAwareSearchQueryElementFactory;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexValueFieldContext;
import org.hibernate.search.backend.lucene.types.codec.impl.AbstractLuceneNumericFieldCodec;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneStandardFieldCodec;
import org.hibernate.search.backend.lucene.types.sort.comparatorsource.impl.LuceneFieldComparatorSource;
import org.hibernate.search.backend.lucene.types.sort.comparatorsource.impl.LuceneNumericFieldComparatorSource;
import org.hibernate.search.backend.lucene.types.sort.comparatorsource.impl.LuceneTextFieldComparatorSource;
import org.hibernate.search.engine.backend.types.converter.spi.DslConverter;
import org.hibernate.search.engine.search.common.SortMode;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.sort.SearchSort;
import org.hibernate.search.engine.search.sort.dsl.SortOrder;
import org.hibernate.search.engine.search.sort.spi.FieldSortBuilder;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.SortField;
import org.apache.lucene.util.BytesRef;

public class LuceneStandardFieldSort extends AbstractLuceneDocumentValueSort {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private LuceneStandardFieldSort(AbstractBuilder<?, ?, ?> builder) {
		super( builder );
	}

	abstract static class AbstractFactory<F, E, C extends LuceneStandardFieldCodec<F, E>>
			extends AbstractLuceneCodecAwareSearchQueryElementFactory<FieldSortBuilder, F, C> {
		protected AbstractFactory(C codec) {
			super( codec );
		}
	}

	/**
	 * @param <F> The field type exposed to the mapper.
	 * @param <E> The encoded type.
	 * @param <C> The codec type.
	 * @see LuceneStandardFieldCodec
	 */
	abstract static class AbstractBuilder<F, E, C extends LuceneStandardFieldCodec<F, E>>
			extends AbstractLuceneDocumentValueSort.AbstractBuilder
			implements FieldSortBuilder {
		protected final LuceneSearchIndexValueFieldContext<F> field;
		protected final C codec;
		private final Object sortMissingValueFirstPlaceholder;
		private final Object sortMissingValueLastPlaceholder;

		protected Object missingValue = SortMissingValue.MISSING_LAST;

		protected AbstractBuilder(LuceneSearchIndexScope<?> scope,
				LuceneSearchIndexValueFieldContext<F> field, C codec,
				Object sortMissingValueFirstPlaceholder, Object sortMissingValueLastPlaceholder) {
			super( scope, field );
			this.field = field;
			this.codec = codec;
			this.sortMissingValueFirstPlaceholder = sortMissingValueFirstPlaceholder;
			this.sortMissingValueLastPlaceholder = sortMissingValueLastPlaceholder;
		}

		@Override
		public void missingFirst() {
			missingValue = SortMissingValue.MISSING_FIRST;
		}

		@Override
		public void missingLast() {
			missingValue = SortMissingValue.MISSING_LAST;
		}

		@Override
		public void missingHighest() {
			missingValue = SortMissingValue.MISSING_HIGHEST;
		}

		@Override
		public void missingLowest() {
			missingValue = SortMissingValue.MISSING_LOWEST;
		}

		@Override
		public void missingAs(Object value, ValueConvert convert) {
			DslConverter<?, ? extends F> dslToIndexConverter = field.type().dslConverter( convert );
			try {
				F converted = dslToIndexConverter.unknownTypeToDocumentValue( value, scope.toDocumentValueConvertContext() );
				missingValue = encodeMissingAs( converted );
			}
			catch (RuntimeException e) {
				throw log.cannotConvertDslParameter( e.getMessage(), e, getEventContext() );
			}
		}

		@Override
		public SearchSort build() {
			return new LuceneStandardFieldSort( this );
		}

		protected Object encodeMissingAs(F converted) {
			return codec.encode( converted );
		}

		protected final Object getEffectiveMissingValue() {
			Object effectiveMissingValue;
			if ( missingValue == SortMissingValue.MISSING_FIRST ) {
				effectiveMissingValue = order == SortOrder.DESC
						? sortMissingValueLastPlaceholder
						: sortMissingValueFirstPlaceholder;
			}
			else if ( missingValue == SortMissingValue.MISSING_LAST ) {
				effectiveMissingValue = order == SortOrder.DESC
						? sortMissingValueFirstPlaceholder
						: sortMissingValueLastPlaceholder;
			}
			else if ( missingValue == SortMissingValue.MISSING_LOWEST ) {
				effectiveMissingValue = sortMissingValueFirstPlaceholder;
			}
			else if ( missingValue == SortMissingValue.MISSING_HIGHEST ) {
				effectiveMissingValue = sortMissingValueLastPlaceholder;
			}
			else {
				effectiveMissingValue = missingValue;
			}
			return effectiveMissingValue;
		}
	}

	public static class NumericFieldFactory<F, E extends Number>
			extends AbstractFactory<F, E, AbstractLuceneNumericFieldCodec<F, E>> {
		public NumericFieldFactory(AbstractLuceneNumericFieldCodec<F, E> codec) {
			super( codec );
		}

		@Override
		public FieldSortBuilder create(LuceneSearchIndexScope<?> scope, LuceneSearchIndexValueFieldContext<F> field) {
			return new NumericFieldBuilder<>( codec, scope, field );
		}
	}

	private static class NumericFieldBuilder<F, E extends Number>
			extends AbstractBuilder<F, E, AbstractLuceneNumericFieldCodec<F, E>> {
		private NumericFieldBuilder(AbstractLuceneNumericFieldCodec<F, E> codec, LuceneSearchIndexScope<?> scope,
				LuceneSearchIndexValueFieldContext<F> field) {
			super( scope, field, codec, codec.getDomain().getMinValue(), codec.getDomain().getMaxValue() );
		}

		@Override
		@SuppressWarnings("unchecked")
		protected LuceneFieldComparatorSource toFieldComparatorSource() {
			return new LuceneNumericFieldComparatorSource<>( nestedDocumentPath, codec.getDomain(),
					(E) getEffectiveMissingValue(), getMultiValueMode(), getNestedFilter() );
		}
	}

	public static class TextFieldFactory<F>
			extends AbstractFactory<F, String, LuceneStandardFieldCodec<F, String>> {
		public TextFieldFactory(LuceneStandardFieldCodec<F, String> codec) {
			super( codec );
		}

		@Override
		public FieldSortBuilder create(LuceneSearchIndexScope<?> scope, LuceneSearchIndexValueFieldContext<F> field) {
			return new TextFieldBuilder<>( codec, scope, field );
		}
	}

	private static class TextFieldBuilder<F> extends AbstractBuilder<F, String, LuceneStandardFieldCodec<F, String>> {
		private TextFieldBuilder(LuceneStandardFieldCodec<F, String> codec, LuceneSearchIndexScope<?> scope,
				LuceneSearchIndexValueFieldContext<F> field) {
			super( scope, field, codec, SortField.STRING_FIRST, SortField.STRING_LAST );
		}

		@Override
		protected Object encodeMissingAs(F converted) {
			return normalize( codec.encode( converted ) );
		}

		@Override
		public void mode(SortMode mode) {
			switch ( mode ) {
				case MIN:
				case MAX:
					super.mode( mode );
					break;
				case SUM:
				case AVG:
				case MEDIAN:
				default:
					throw log.invalidSortModeForStringField( mode, getEventContext() );
			}
		}

		@Override
		protected LuceneFieldComparatorSource toFieldComparatorSource() {
			return new LuceneTextFieldComparatorSource( nestedDocumentPath, missingValue,
					getMultiValueMode(), getNestedFilter() );
		}

		private BytesRef normalize(String value) {
			if ( value == null ) {
				return null;
			}
			Analyzer searchAnalyzerOrNormalizer = field.type().searchAnalyzerOrNormalizer();
			return searchAnalyzerOrNormalizer.normalize( absoluteFieldPath, value );
		}
	}

	public static class TemporalFieldFactory<F extends TemporalAccessor, E extends Number>
			extends AbstractFactory<F, E, AbstractLuceneNumericFieldCodec<F, E>> {
		public TemporalFieldFactory(AbstractLuceneNumericFieldCodec<F, E> codec) {
			super( codec );
		}

		@Override
		public FieldSortBuilder create(LuceneSearchIndexScope<?> scope, LuceneSearchIndexValueFieldContext<F> field) {
			return new TemporalFieldBuilder<>( codec, scope, field );
		}
	}

	private static class TemporalFieldBuilder<F extends TemporalAccessor, E extends Number>
			extends NumericFieldBuilder<F, E> {
		private TemporalFieldBuilder(AbstractLuceneNumericFieldCodec<F, E> codec, LuceneSearchIndexScope<?> scope,
				LuceneSearchIndexValueFieldContext<F> field) {
			super( codec, scope, field );
		}

		@Override
		public void mode(SortMode mode) {
			switch ( mode ) {
				case MIN:
				case MAX:
				case AVG:
				case MEDIAN:
					super.mode( mode );
					break;
				case SUM:
					throw log.invalidSortModeForTemporalField( mode, getEventContext() );
			}
		}
	}
}
