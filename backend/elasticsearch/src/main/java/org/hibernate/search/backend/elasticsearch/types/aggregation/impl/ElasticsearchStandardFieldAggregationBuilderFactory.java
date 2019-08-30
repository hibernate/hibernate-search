/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.aggregation.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.search.aggregation.impl.ElasticsearchRangeAggregation;
import org.hibernate.search.backend.elasticsearch.search.aggregation.impl.ElasticsearchTermsAggregation;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchContext;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.engine.backend.types.converter.FromDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.ToDocumentFieldValueConverter;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.aggregation.spi.RangeAggregationBuilder;
import org.hibernate.search.engine.search.aggregation.spi.TermsAggregationBuilder;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class ElasticsearchStandardFieldAggregationBuilderFactory<F>
		implements ElasticsearchFieldAggregationBuilderFactory {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final boolean aggregable;

	private final ToDocumentFieldValueConverter<?, ? extends F> toFieldValueConverter;
	private final ToDocumentFieldValueConverter<? super F, ? extends F> rawToFieldValueConverter;
	private final FromDocumentFieldValueConverter<? super F, ?> fromFieldValueConverter;
	private final FromDocumentFieldValueConverter<? super F, F> rawFromFieldValueConverter;
	private final ElasticsearchFieldCodec<F> codec;

	public ElasticsearchStandardFieldAggregationBuilderFactory(boolean aggregable,
			ToDocumentFieldValueConverter<?, ? extends F> toFieldValueConverter,
			ToDocumentFieldValueConverter<? super F, ? extends F> rawToFieldValueConverter,
			FromDocumentFieldValueConverter<? super F, ?> fromFieldValueConverter,
			FromDocumentFieldValueConverter<? super F, F> rawFromFieldValueConverter,
			ElasticsearchFieldCodec<F> codec) {
		this.aggregable = aggregable;
		this.toFieldValueConverter = toFieldValueConverter;
		this.rawToFieldValueConverter = rawToFieldValueConverter;
		this.fromFieldValueConverter = fromFieldValueConverter;
		this.rawFromFieldValueConverter = rawFromFieldValueConverter;
		this.codec = codec;
	}

	@Override
	public <K> TermsAggregationBuilder<K> createTermsAggregationBuilder(ElasticsearchSearchContext searchContext,
			String absoluteFieldPath, Class<K> expectedType, ValueConvert convert) {
		checkAggregable( absoluteFieldPath, aggregable );

		FromDocumentFieldValueConverter<? super F, ? extends K> fromFieldValueConverter = getFromFieldValueConverter(
				absoluteFieldPath, expectedType, convert
		);

		return new ElasticsearchTermsAggregation.Builder<>(
				searchContext, absoluteFieldPath, fromFieldValueConverter, codec
		);
	}

	@Override
	public <K> RangeAggregationBuilder<K> createRangeAggregationBuilder(ElasticsearchSearchContext searchContext,
			String absoluteFieldPath, Class<K> expectedType, ValueConvert convert) {
		checkAggregable( absoluteFieldPath, aggregable );

		ToDocumentFieldValueConverter<?, ? extends F> toFieldValueConverter = getToFieldValueConverter(
				absoluteFieldPath, expectedType, convert
		);

		return new ElasticsearchRangeAggregation.Builder<>(
				searchContext, absoluteFieldPath, toFieldValueConverter, codec
		);
	}

	@Override
	public boolean hasCompatibleCodec(ElasticsearchFieldAggregationBuilderFactory other) {
		if ( !getClass().equals( other.getClass() ) ) {
			return false;
		}
		ElasticsearchStandardFieldAggregationBuilderFactory<?> castedOther =
				(ElasticsearchStandardFieldAggregationBuilderFactory<?>) other;
		return aggregable == castedOther.aggregable && codec.isCompatibleWith( castedOther.codec );
	}

	@Override
	public boolean hasCompatibleConverter(ElasticsearchFieldAggregationBuilderFactory other) {
		if ( !getClass().equals( other.getClass() ) ) {
			return false;
		}
		ElasticsearchStandardFieldAggregationBuilderFactory<?> castedOther =
				(ElasticsearchStandardFieldAggregationBuilderFactory<?>) other;
		return toFieldValueConverter.isCompatibleWith( castedOther.toFieldValueConverter )
				&& fromFieldValueConverter.isCompatibleWith( castedOther.fromFieldValueConverter );
	}

	private static void checkAggregable(String absoluteFieldPath, boolean aggregable) {
		if ( !aggregable ) {
				throw log.nonAggregableField( absoluteFieldPath,
						EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath ) );
		}
	}

	private <T> ToDocumentFieldValueConverter<?, ? extends F> getToFieldValueConverter(
			String absoluteFieldPath, Class<T> expectedType, ValueConvert convert) {
		ToDocumentFieldValueConverter<?, ? extends F> result;
		switch ( convert ) {
			case NO:
				result = rawToFieldValueConverter;
				break;
			case YES:
			default:
				result = toFieldValueConverter;
				break;
		}
		if ( !result.isValidInputType( expectedType ) ) {
			throw log.invalidAggregationInvalidType(
					absoluteFieldPath, expectedType, EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath )
			);
		}
		return result;
	}

	@SuppressWarnings("unchecked") // We check the cast is legal by asking the converter
	private <T> FromDocumentFieldValueConverter<? super F, ? extends T> getFromFieldValueConverter(
			String absoluteFieldPath, Class<T> expectedType, ValueConvert convert) {
		FromDocumentFieldValueConverter<? super F, ?> result;
		switch ( convert ) {
			case NO:
				result = rawFromFieldValueConverter;
				break;
			case YES:
			default:
				result = fromFieldValueConverter;
				break;
		}
		if ( !result.isConvertedTypeAssignableTo( expectedType ) ) {
			throw log.invalidAggregationInvalidType(
					absoluteFieldPath, expectedType, EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath )
			);
		}
		return (FromDocumentFieldValueConverter<? super F, ? extends T>) result;
	}
}
