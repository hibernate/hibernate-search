/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.aggregation.impl;

import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchFieldContext;
import org.hibernate.search.backend.lucene.types.codec.impl.AbstractLuceneNumericFieldCodec;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.backend.types.converter.spi.DslConverter;
import org.hibernate.search.engine.search.aggregation.spi.RangeAggregationBuilder;
import org.hibernate.search.engine.search.aggregation.spi.TermsAggregationBuilder;
import org.hibernate.search.engine.search.common.ValueConvert;

public class LuceneNumericFieldAggregationBuilderFactory<F>
		extends AbstractLuceneFieldAggregationBuilderFactory<F> {

	private final AbstractLuceneNumericFieldCodec<F, ?> codec;

	public LuceneNumericFieldAggregationBuilderFactory(boolean aggregable,
			AbstractLuceneNumericFieldCodec<F, ?> codec) {
		super( aggregable );
		this.codec = codec;
	}

	@Override
	public <K> TermsAggregationBuilder<K> createTermsAggregationBuilder(LuceneSearchContext searchContext,
			LuceneSearchFieldContext<F> field, Class<K> expectedType, ValueConvert convert) {
		checkAggregable( field );

		ProjectionConverter<? super F, ? extends K> fromFieldValueConverter =
				getFromFieldValueConverter( field, expectedType, convert );

		return new LuceneNumericTermsAggregation.Builder<>( searchContext, field, fromFieldValueConverter, getCodec() );
	}

	@Override
	public <K> RangeAggregationBuilder<K> createRangeAggregationBuilder(LuceneSearchContext searchContext,
			LuceneSearchFieldContext<F> field, Class<K> expectedType, ValueConvert convert) {
		checkAggregable( field );

		DslConverter<?, ? extends F> toFieldValueConverter =
				getToFieldValueConverter( field, expectedType, convert );
		// TODO HSEARCH-3945 This is legacy behavior to trigger a failure when the projection converter is different.
		//   It's not strictly necessary but is expected in tests.
		//   Maybe relax the constraint?
		if ( ValueConvert.YES.equals( convert ) ) {
			field.type().projectionConverter();
		}

		return new LuceneNumericRangeAggregation.Builder<>( searchContext, field, toFieldValueConverter, codec );
	}

	@Override
	protected AbstractLuceneNumericFieldCodec<F, ?> getCodec() {
		return codec;
	}
}
