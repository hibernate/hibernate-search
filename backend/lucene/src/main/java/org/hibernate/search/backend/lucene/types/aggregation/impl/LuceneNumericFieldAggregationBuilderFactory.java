/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.aggregation.impl;

import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.types.codec.impl.AbstractLuceneNumericFieldCodec;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.backend.types.converter.spi.DslConverter;
import org.hibernate.search.engine.search.aggregation.spi.RangeAggregationBuilder;
import org.hibernate.search.engine.search.aggregation.spi.TermsAggregationBuilder;
import org.hibernate.search.engine.search.common.ValueConvert;

public class LuceneNumericFieldAggregationBuilderFactory<F>
		extends AbstractLuceneStandardFieldAggregationBuilderFactory<F> {

	private final AbstractLuceneNumericFieldCodec<F, ?> codec;

	public LuceneNumericFieldAggregationBuilderFactory(boolean aggregable,
			DslConverter<?, ? extends F> toFieldValueConverter,
			DslConverter<? super F, ? extends F> rawToFieldValueConverter,
			ProjectionConverter<? super F, ?> fromFieldValueConverter,
			ProjectionConverter<? super F, F> rawFromFieldValueConverter,
			AbstractLuceneNumericFieldCodec<F, ?> codec) {
		super( aggregable, toFieldValueConverter, rawToFieldValueConverter, fromFieldValueConverter,
				rawFromFieldValueConverter );
		this.codec = codec;
	}

	@Override
	public <K> TermsAggregationBuilder<K> createTermsAggregationBuilder(LuceneSearchContext searchContext,
			String nestedDocumentPath, String absoluteFieldPath, Class<K> expectedType, ValueConvert convert) {
		checkAggregable( absoluteFieldPath );

		ProjectionConverter<? super F, ? extends K> fromFieldValueConverter =
				getFromFieldValueConverter( absoluteFieldPath, expectedType, convert );

		return new LuceneNumericTermsAggregation.Builder<>(
				searchContext, nestedDocumentPath, absoluteFieldPath, fromFieldValueConverter, getCodec()
		);
	}

	@Override
	public <K> RangeAggregationBuilder<K> createRangeAggregationBuilder(LuceneSearchContext searchContext,
			String nestedDocumentPath, String absoluteFieldPath,
			Class<K> expectedType, ValueConvert convert) {
		checkAggregable( absoluteFieldPath );

		DslConverter<?, ? extends F> toFieldValueConverter =
				getToFieldValueConverter( absoluteFieldPath, expectedType, convert );

		return new LuceneNumericRangeAggregation.Builder<>(
				searchContext, nestedDocumentPath, absoluteFieldPath,
				toFieldValueConverter, codec
		);
	}

	@Override
	protected AbstractLuceneNumericFieldCodec<F, ?> getCodec() {
		return codec;
	}
}
