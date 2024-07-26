/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.types.dsl.impl;

import org.hibernate.search.backend.lucene.search.common.impl.AbstractLuceneCodecAwareSearchQueryElementFactory;
import org.hibernate.search.backend.lucene.types.aggregation.impl.AbstractLuceneMetricCompensatedSumAggregation;
import org.hibernate.search.backend.lucene.types.codec.impl.AbstractLuceneNumericFieldCodec;
import org.hibernate.search.backend.lucene.types.codec.impl.DocValues;
import org.hibernate.search.backend.lucene.types.codec.impl.Indexing;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneFloatFieldCodec;
import org.hibernate.search.backend.lucene.types.codec.impl.Storage;
import org.hibernate.search.engine.backend.types.converter.spi.DefaultStringConverters;
import org.hibernate.search.engine.search.aggregation.spi.FieldMetricAggregationBuilder;

class LuceneFloatIndexFieldTypeOptionsStep
		extends AbstractLuceneNumericIndexFieldTypeOptionsStep<LuceneFloatIndexFieldTypeOptionsStep, Float> {

	LuceneFloatIndexFieldTypeOptionsStep(LuceneIndexFieldTypeBuildContext buildContext) {
		super( buildContext, Float.class, DefaultStringConverters.FLOAT );
	}

	@Override
	protected LuceneFloatIndexFieldTypeOptionsStep thisAsS() {
		return this;
	}

	@Override
	protected AbstractLuceneNumericFieldCodec<Float, ?> createCodec(Indexing indexing, DocValues docValues,
			Storage storage, Float indexNullAsValue) {
		return new LuceneFloatFieldCodec( indexing, docValues, storage, indexNullAsValue );
	}

	@Override
	protected AbstractLuceneCodecAwareSearchQueryElementFactory<FieldMetricAggregationBuilder.TypeSelector,
			Float,
			AbstractLuceneNumericFieldCodec<Float, ?>> sumAvgMetricAggregationFactory(
					AbstractLuceneNumericFieldCodec<Float, ?> codec, String operation) {
		return new AbstractLuceneMetricCompensatedSumAggregation.Factory<>( codec, operation );
	}
}
