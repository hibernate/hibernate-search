/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.aggregation.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchContext;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.engine.backend.types.converter.FromDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.ToDocumentFieldValueConverter;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.aggregation.spi.RangeAggregationBuilder;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class ElasticsearchBooleanFieldAggregationBuilderFactory
		extends ElasticsearchStandardFieldAggregationBuilderFactory<Boolean> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	public ElasticsearchBooleanFieldAggregationBuilderFactory(boolean aggregable,
			ToDocumentFieldValueConverter<?, ? extends Boolean> toFieldValueConverter,
			ToDocumentFieldValueConverter<? super Boolean, ? extends Boolean> rawToFieldValueConverter,
			FromDocumentFieldValueConverter<? super Boolean, ?> fromFieldValueConverter,
			FromDocumentFieldValueConverter<? super Boolean, Boolean> rawFromFieldValueConverter,
			ElasticsearchFieldCodec<Boolean> codec) {
		super( aggregable, toFieldValueConverter, rawToFieldValueConverter, fromFieldValueConverter,
				rawFromFieldValueConverter, codec
		);
	}

	@Override
	public <K> RangeAggregationBuilder<K> createRangeAggregationBuilder(ElasticsearchSearchContext searchContext,
			String absoluteFieldPath, Class<K> expectedType, ValueConvert convert) {
		throw log.rangeAggregationsNotSupportedByFieldType(
				EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath )
		);
	}
}
