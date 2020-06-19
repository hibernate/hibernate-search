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
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchFieldContext;
import org.hibernate.search.backend.lucene.types.codec.impl.AbstractLuceneNumericFieldCodec;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneStandardFieldCodec;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneTextFieldCodec;
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

import org.apache.lucene.search.SortField;

class LuceneStandardFieldSort extends AbstractLuceneDocumentValueSort {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	protected LuceneStandardFieldSort(AbstractBuilder builder) {
		super( builder );
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
		private final LuceneSearchFieldContext<F> field;
		protected final C codec;
		private final Object sortMissingValueFirstPlaceholder;
		private final Object sortMissingValueLastPlaceholder;

		protected Object missingValue = SortMissingValue.MISSING_LAST;

		protected AbstractBuilder(LuceneSearchContext searchContext,
				LuceneSearchFieldContext<F> field, C codec,
				Object sortMissingValueFirstPlaceholder, Object sortMissingValueLastPlaceholder) {
			super( searchContext, field );
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
		public void missingAs(Object value, ValueConvert convert) {
			DslConverter<?, ? extends F> dslToIndexConverter = field.type().dslConverter( convert );
			try {
				F converted = dslToIndexConverter.convertUnknown( value, searchContext.toDocumentFieldValueConvertContext() );
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
				effectiveMissingValue = order == SortOrder.DESC ? sortMissingValueLastPlaceholder
						: sortMissingValueFirstPlaceholder;
			}
			else if ( missingValue == SortMissingValue.MISSING_LAST ) {
				effectiveMissingValue = order == SortOrder.DESC ? sortMissingValueFirstPlaceholder
						: sortMissingValueLastPlaceholder;
			}
			else {
				effectiveMissingValue = missingValue;
			}
			return effectiveMissingValue;
		}
	}

	public static class NumericFieldBuilder<F, E extends Number>
			extends AbstractBuilder<F, E, AbstractLuceneNumericFieldCodec<F, E>> {
		NumericFieldBuilder(LuceneSearchContext searchContext, LuceneSearchFieldContext<F> field,
				AbstractLuceneNumericFieldCodec<F, E> codec) {
			super( searchContext, field, codec, codec.getDomain().getMinValue(), codec.getDomain().getMaxValue() );
		}

		@Override
		protected LuceneFieldComparatorSource toFieldComparatorSource() {
			return new LuceneNumericFieldComparatorSource<>( nestedDocumentPath, codec.getDomain(),
					(E) getEffectiveMissingValue(), getMultiValueMode(), getNestedFilter() );
		}
	}

	public static class TextFieldBuilder<F> extends AbstractBuilder<F, String, LuceneTextFieldCodec<F>> {
		TextFieldBuilder(LuceneSearchContext searchContext, LuceneSearchFieldContext<F> field,
				LuceneTextFieldCodec<F> codec) {
			super( searchContext, field, codec, SortField.STRING_FIRST, SortField.STRING_LAST );
		}

		@Override
		protected Object encodeMissingAs(F converted) {
			return codec.normalize( absoluteFieldPath, codec.encode( converted ) );
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
					throw log.cannotComputeSumOrAvgOrMedianForStringField( getEventContext() );
			}
		}

		@Override
		protected LuceneFieldComparatorSource toFieldComparatorSource() {
			return new LuceneTextFieldComparatorSource( nestedDocumentPath, missingValue,
					getMultiValueMode(), getNestedFilter() );
		}
	}

	public static class TemporalFieldBuilder<F extends TemporalAccessor, E extends Number>
			extends NumericFieldBuilder<F, E> {
		TemporalFieldBuilder(LuceneSearchContext searchContext, LuceneSearchFieldContext<F> field,
				AbstractLuceneNumericFieldCodec<F, E> codec) {
			super( searchContext, field, codec );
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
					throw log.cannotComputeSumForTemporalField( getEventContext() );
			}
		}
	}
}
