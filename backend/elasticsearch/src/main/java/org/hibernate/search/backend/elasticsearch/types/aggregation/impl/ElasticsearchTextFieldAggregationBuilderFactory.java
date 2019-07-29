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
import org.hibernate.search.engine.search.aggregation.spi.TermsAggregationBuilder;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class ElasticsearchTextFieldAggregationBuilderFactory
		extends ElasticsearchStandardFieldAggregationBuilderFactory<String> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final boolean tokenized;

	public ElasticsearchTextFieldAggregationBuilderFactory(boolean aggregable,
			ToDocumentFieldValueConverter<?, ? extends String> toFieldValueConverter,
			ToDocumentFieldValueConverter<? super String, ? extends String> rawToFieldValueConverter,
			FromDocumentFieldValueConverter<? super String, ?> fromFieldValueConverter,
			FromDocumentFieldValueConverter<? super String, String> rawFromFieldValueConverter,
			ElasticsearchFieldCodec<String> codec,
			boolean tokenized) {
		super( aggregable, toFieldValueConverter, rawToFieldValueConverter, fromFieldValueConverter,
				rawFromFieldValueConverter, codec
		);
		this.tokenized = tokenized;
	}

	@Override
	public <K> TermsAggregationBuilder<K> createTermsAggregationBuilder(ElasticsearchSearchContext searchContext,
			String absoluteFieldPath, Class<K> expectedType, ValueConvert convert) {
		if ( tokenized ) {
			throw log.termsAggregationsNotSupportedByAnalyzedTextFieldType(
					EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath )
			);
		}
		return super.createTermsAggregationBuilder( searchContext, absoluteFieldPath, expectedType, convert );
	}

	@Override
	public <K> RangeAggregationBuilder<K> createRangeAggregationBuilder(ElasticsearchSearchContext searchContext,
			String absoluteFieldPath, Class<K> expectedType, ValueConvert convert) {
		throw log.rangeAggregationsNotSupportedByFieldType(
				EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath )
		);
	}
}
