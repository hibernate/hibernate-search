/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.types.dsl.impl;

import org.hibernate.search.backend.lucene.search.common.impl.AbstractLuceneCodecAwareSearchQueryElementFactory;
import org.hibernate.search.backend.lucene.types.aggregation.impl.LuceneAvgCompensatedSumAggregation;
import org.hibernate.search.backend.lucene.types.aggregation.impl.LuceneSumCompensatedSumAggregation;
import org.hibernate.search.backend.lucene.types.codec.impl.AbstractLuceneNumericFieldCodec;
import org.hibernate.search.backend.lucene.types.codec.impl.DocValues;
import org.hibernate.search.backend.lucene.types.codec.impl.Indexing;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneDoubleFieldCodec;
import org.hibernate.search.backend.lucene.types.codec.impl.Storage;
import org.hibernate.search.engine.backend.types.converter.spi.DefaultStringConverters;
import org.hibernate.search.engine.search.aggregation.spi.FieldMetricAggregationBuilder;

class LuceneDoubleIndexFieldTypeOptionsStep
		extends AbstractLuceneNumericIndexFieldTypeOptionsStep<LuceneDoubleIndexFieldTypeOptionsStep, Double> {

	LuceneDoubleIndexFieldTypeOptionsStep(LuceneIndexFieldTypeBuildContext buildContext) {
		super( buildContext, Double.class, DefaultStringConverters.DOUBLE );
	}

	@Override
	protected LuceneDoubleIndexFieldTypeOptionsStep thisAsS() {
		return this;
	}

	@Override
	protected AbstractLuceneNumericFieldCodec<Double, ?> createCodec(Indexing indexing, DocValues docValues,
			Storage storage, Double indexNullAsValue) {
		return new LuceneDoubleFieldCodec( indexing, docValues, storage, indexNullAsValue );
	}

	@Override
	protected AbstractLuceneCodecAwareSearchQueryElementFactory<FieldMetricAggregationBuilder.TypeSelector,
			Double,
			AbstractLuceneNumericFieldCodec<Double, ?>> sumMetricAggregationFactory(
					AbstractLuceneNumericFieldCodec<Double, ?> codec) {
		return LuceneSumCompensatedSumAggregation.factory( codec );
	}

	@Override
	protected AbstractLuceneCodecAwareSearchQueryElementFactory<FieldMetricAggregationBuilder.TypeSelector,
			Double,
			AbstractLuceneNumericFieldCodec<Double, ?>> avgMetricAggregationFactory(
					AbstractLuceneNumericFieldCodec<Double, ?> codec) {
		return LuceneAvgCompensatedSumAggregation.factory( codec );
	}
}
