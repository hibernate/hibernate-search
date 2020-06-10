/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.aggregation.impl;

import org.hibernate.search.backend.elasticsearch.search.aggregation.impl.ElasticsearchRangeAggregation;
import org.hibernate.search.backend.elasticsearch.search.aggregation.impl.ElasticsearchTermsAggregation;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchContext;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchFieldContext;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.engine.backend.types.converter.spi.DslConverter;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.search.aggregation.spi.RangeAggregationBuilder;
import org.hibernate.search.engine.search.aggregation.spi.TermsAggregationBuilder;
import org.hibernate.search.engine.search.common.ValueConvert;

public class ElasticsearchStandardFieldAggregationBuilderFactory<F>
		extends AbstractElasticsearchFieldAggregationBuilderFactory<F> {

	public ElasticsearchStandardFieldAggregationBuilderFactory(boolean aggregable, ElasticsearchFieldCodec<F> codec) {
		super( aggregable, codec );
	}

	@Override
	public <K> TermsAggregationBuilder<K> createTermsAggregationBuilder(ElasticsearchSearchContext searchContext,
			ElasticsearchSearchFieldContext<F> field, Class<K> expectedType, ValueConvert convert) {
		checkAggregable( field );

		ProjectionConverter<? super F, ? extends K> fromFieldValueConverter = getFromFieldValueConverter(
				field, expectedType, convert
		);

		return new ElasticsearchTermsAggregation.Builder<>(
				searchContext, field, fromFieldValueConverter, codec
		);
	}

	@Override
	public <K> RangeAggregationBuilder<K> createRangeAggregationBuilder(ElasticsearchSearchContext searchContext,
			ElasticsearchSearchFieldContext<F> field, Class<K> expectedType, ValueConvert convert) {
		checkAggregable( field );

		DslConverter<?, ? extends F> toFieldValueConverter = getToFieldValueConverter(
				field, expectedType, convert
		);
		// TODO HSEARCH-3945 This is legacy behavior to trigger a failure when the projection converter is different.
		//   It's not strictly necessary but is expected in tests.
		//   Maybe relax the constraint?
		if ( ValueConvert.YES.equals( convert ) ) {
			field.type().projectionConverter();
		}

		return new ElasticsearchRangeAggregation.Builder<>(
				searchContext, field, toFieldValueConverter, codec
		);
	}

	private <T> DslConverter<?, ? extends F> getToFieldValueConverter(
			ElasticsearchSearchFieldContext<F> field, Class<T> expectedType, ValueConvert convert) {
		DslConverter<?, ? extends F> result = field.type().dslConverter( convert );
		if ( !result.isValidInputType( expectedType ) ) {
			throw log.invalidAggregationInvalidType( field.absolutePath(), expectedType, field.eventContext() );
		}
		return result;
	}

	@SuppressWarnings("unchecked") // We check the cast is legal by asking the converter
	private <T> ProjectionConverter<? super F, ? extends T> getFromFieldValueConverter(
			ElasticsearchSearchFieldContext<F> field, Class<T> expectedType, ValueConvert convert) {
		ProjectionConverter<? super F, ?> result = field.type().projectionConverter( convert );
		if ( !result.isConvertedTypeAssignableTo( expectedType ) ) {
			throw log.invalidAggregationInvalidType( field.absolutePath(), expectedType, field.eventContext() );
		}
		return (ProjectionConverter<? super F, ? extends T>) result;
	}
}
