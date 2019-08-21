/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.aggregation.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneFieldCodec;
import org.hibernate.search.engine.backend.types.converter.FromDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.ToDocumentFieldValueConverter;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.aggregation.spi.RangeAggregationBuilder;
import org.hibernate.search.engine.search.aggregation.spi.TermsAggregationBuilder;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class LuceneTextFieldAggregationBuilderFactory
		extends AbstractLuceneStandardFieldAggregationBuilderFactory<String> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final LuceneFieldCodec<String> codec;
	private final boolean tokenized;

	public LuceneTextFieldAggregationBuilderFactory(boolean aggregable,
			ToDocumentFieldValueConverter<?, ? extends String> toFieldValueConverter,
			ToDocumentFieldValueConverter<? super String, ? extends String> rawToFieldValueConverter,
			FromDocumentFieldValueConverter<? super String, ?> fromFieldValueConverter,
			FromDocumentFieldValueConverter<? super String, String> rawFromFieldValueConverter,
			LuceneFieldCodec<String> codec,
			boolean tokenized) {
		super( aggregable, toFieldValueConverter, rawToFieldValueConverter, fromFieldValueConverter,
				rawFromFieldValueConverter
		);
		this.codec = codec;
		this.tokenized = tokenized;
	}

	@Override
	public <K> TermsAggregationBuilder<K> createTermsAggregationBuilder(LuceneSearchContext searchContext,
			String absoluteFieldPath, Class<K> expectedType, ValueConvert convert) {
		if ( tokenized ) {
			throw log.termsAggregationsNotSupportedByAnalyzedTextFieldType(
					EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath )
			);
		}

		checkAggregable( absoluteFieldPath );

		FromDocumentFieldValueConverter<? super String, ? extends K> fromFieldValueConverter =
				getFromFieldValueConverter( absoluteFieldPath, expectedType, convert );

		return new LuceneTextTermsAggregation.Builder<>(
				searchContext, absoluteFieldPath, fromFieldValueConverter
		);
	}

	@Override
	public <K> RangeAggregationBuilder<K> createRangeAggregationBuilder(LuceneSearchContext searchContext,
			String absoluteFieldPath, Class<K> expectedType, ValueConvert convert) {
		throw log.rangeAggregationsNotSupportedByFieldType(
				EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath )
		);
	}

	@Override
	protected LuceneFieldCodec<String> getCodec() {
		return codec;
	}
}
