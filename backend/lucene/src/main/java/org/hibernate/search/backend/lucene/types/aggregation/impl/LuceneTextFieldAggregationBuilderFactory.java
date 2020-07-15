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
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchFieldContext;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneFieldCodec;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.search.aggregation.spi.RangeAggregationBuilder;
import org.hibernate.search.engine.search.aggregation.spi.TermsAggregationBuilder;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class LuceneTextFieldAggregationBuilderFactory
		extends AbstractLuceneFieldAggregationBuilderFactory<String> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final LuceneFieldCodec<String> codec;

	public LuceneTextFieldAggregationBuilderFactory(boolean aggregable, LuceneFieldCodec<String> codec) {
		super( aggregable );
		this.codec = codec;
	}

	@Override
	public <K> TermsAggregationBuilder<K> createTermsAggregationBuilder(LuceneSearchContext searchContext,
			LuceneSearchFieldContext<String> field, Class<K> expectedType, ValueConvert convert) {
		if ( field.type().searchAnalyzerName().isPresent() ) {
			throw log.termsAggregationsNotSupportedByAnalyzedTextFieldType( field.eventContext() );
		}

		checkAggregable( field );

		ProjectionConverter<? super String, ? extends K> fromFieldValueConverter = field.type()
				.projectionConverter( convert ).withConvertedType( expectedType, field );

		return new LuceneTextTermsAggregation.Builder<>( searchContext, field, fromFieldValueConverter );
	}

	@Override
	public <K> RangeAggregationBuilder<K> createRangeAggregationBuilder(LuceneSearchContext searchContext,
			LuceneSearchFieldContext<String> field, Class<K> expectedType, ValueConvert convert) {
		throw log.rangeAggregationsNotSupportedByFieldType( field.eventContext() );
	}

	@Override
	protected LuceneFieldCodec<String> getCodec() {
		return codec;
	}
}
