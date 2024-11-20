/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import java.math.BigInteger;

import org.hibernate.search.backend.elasticsearch.logging.impl.MappingLog;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.DataTypes;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchBigIntegerFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.engine.backend.types.converter.spi.DefaultStringConverters;
import org.hibernate.search.engine.backend.types.dsl.ScaledNumberIndexFieldTypeOptionsStep;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexFieldTypeDefaultsProvider;

class ElasticsearchBigIntegerIndexFieldTypeOptionsStep
		extends AbstractElasticsearchNumericFieldTypeOptionsStep<ElasticsearchBigIntegerIndexFieldTypeOptionsStep, BigInteger>
		implements ScaledNumberIndexFieldTypeOptionsStep<ElasticsearchBigIntegerIndexFieldTypeOptionsStep, BigInteger> {

	private final IndexFieldTypeDefaultsProvider defaultsProvider;

	private Integer decimalScale = null;

	ElasticsearchBigIntegerIndexFieldTypeOptionsStep(ElasticsearchIndexFieldTypeBuildContext buildContext,
			IndexFieldTypeDefaultsProvider defaultsProvider) {
		super( buildContext, BigInteger.class, DataTypes.SCALED_FLOAT, DefaultStringConverters.BIG_INTEGER );
		this.defaultsProvider = defaultsProvider;
	}

	@Override
	public ElasticsearchBigIntegerIndexFieldTypeOptionsStep decimalScale(int decimalScale) {
		this.decimalScale = decimalScale;
		return thisAsS();
	}

	@Override
	protected ElasticsearchFieldCodec<BigInteger> completeCodec(ElasticsearchIndexFieldTypeBuildContext buildContext) {
		int resolvedDecimalScale = resolveDecimalScale();

		if ( resolvedDecimalScale > 0 ) {
			throw MappingLog.INSTANCE.invalidDecimalScale( resolvedDecimalScale, this.buildContext.getEventContext() );
		}

		ElasticsearchBigIntegerFieldCodec codec =
				new ElasticsearchBigIntegerFieldCodec( buildContext.getUserFacingGson(), resolvedDecimalScale );
		builder.mapping().setScalingFactor( codec.scalingFactor().doubleValue() );

		return codec;
	}

	@Override
	protected ElasticsearchBigIntegerIndexFieldTypeOptionsStep thisAsS() {
		return this;
	}

	private int resolveDecimalScale() {
		if ( decimalScale != null ) {
			return decimalScale;
		}
		if ( defaultsProvider.decimalScale() != null ) {
			return defaultsProvider.decimalScale();
		}

		throw MappingLog.INSTANCE.nullDecimalScale( buildContext.hints().missingDecimalScale(),
				buildContext.getEventContext() );
	}
}
