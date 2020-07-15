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

		ProjectionConverter<? super F, ? extends K> fromFieldValueConverter = field.type().projectionConverter( convert )
				.withConvertedType( expectedType, field );

		return new ElasticsearchTermsAggregation.Builder<>(
				searchContext, field, fromFieldValueConverter, codec
		);
	}

	@Override
	public <K> RangeAggregationBuilder<K> createRangeAggregationBuilder(ElasticsearchSearchContext searchContext,
			ElasticsearchSearchFieldContext<F> field, Class<K> expectedType, ValueConvert convert) {
		checkAggregable( field );

		DslConverter<? super K, ? extends F> toFieldValueConverter = field.type().dslConverter( convert )
				.withInputType( expectedType, field );

		return new ElasticsearchRangeAggregation.Builder<>(
				searchContext, field, toFieldValueConverter, codec
		);
	}

}
