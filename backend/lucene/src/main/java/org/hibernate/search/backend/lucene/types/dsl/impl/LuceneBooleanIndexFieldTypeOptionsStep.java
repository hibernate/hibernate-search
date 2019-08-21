/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.dsl.impl;

import org.hibernate.search.backend.lucene.types.aggregation.impl.LuceneBooleanFieldAggregationBuilderFactory;
import org.hibernate.search.backend.lucene.types.aggregation.impl.LuceneNumericFieldAggregationBuilderFactory;
import org.hibernate.search.backend.lucene.types.codec.impl.AbstractLuceneNumericFieldCodec;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneBooleanFieldCodec;
import org.hibernate.search.engine.backend.types.converter.FromDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.ToDocumentFieldValueConverter;

class LuceneBooleanIndexFieldTypeOptionsStep
		extends AbstractLuceneNumericIndexFieldTypeOptionsStep<LuceneBooleanIndexFieldTypeOptionsStep, Boolean> {

	LuceneBooleanIndexFieldTypeOptionsStep(LuceneIndexFieldTypeBuildContext buildContext) {
		super( buildContext, Boolean.class );
	}

	@Override
	protected LuceneBooleanIndexFieldTypeOptionsStep thisAsS() {
		return this;
	}

	@Override
	protected AbstractLuceneNumericFieldCodec<Boolean, ?> createCodec(boolean resolvedProjectable,
			boolean resolvedSearchable, boolean resolvedSortable, boolean resolvedAggregable,
			Boolean indexNullAsValue) {
		return new LuceneBooleanFieldCodec(
				resolvedProjectable, resolvedSearchable, resolvedSortable, resolvedAggregable, indexNullAsValue
		);
	}

	@Override
	protected LuceneNumericFieldAggregationBuilderFactory<Boolean> createAggregationBuilderFactory(
			boolean resolvedAggregable, ToDocumentFieldValueConverter<?, ? extends Boolean> dslToIndexConverter,
			ToDocumentFieldValueConverter<Boolean, ? extends Boolean> rawDslToIndexConverter,
			FromDocumentFieldValueConverter<? super Boolean, ?> indexToProjectionConverter,
			FromDocumentFieldValueConverter<? super Boolean, Boolean> rawIndexToProjectionConverter,
			AbstractLuceneNumericFieldCodec<Boolean, ?> codec) {
		return new LuceneBooleanFieldAggregationBuilderFactory(
				resolvedAggregable,
				dslToIndexConverter, rawDslToIndexConverter,
				indexToProjectionConverter, rawIndexToProjectionConverter,
				codec
		);
	}
}
