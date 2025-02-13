/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import java.math.BigDecimal;

import org.hibernate.search.backend.elasticsearch.logging.impl.MappingLog;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.DataTypes;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchBigDecimalFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.engine.backend.types.converter.spi.DefaultStringConverters;
import org.hibernate.search.engine.backend.types.dsl.ScaledNumberIndexFieldTypeOptionsStep;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexFieldTypeDefaultsProvider;

class ElasticsearchBigDecimalIndexFieldTypeOptionsStep
		extends AbstractElasticsearchNumericFieldTypeOptionsStep<ElasticsearchBigDecimalIndexFieldTypeOptionsStep, BigDecimal>
		implements ScaledNumberIndexFieldTypeOptionsStep<ElasticsearchBigDecimalIndexFieldTypeOptionsStep, BigDecimal> {

	private final IndexFieldTypeDefaultsProvider defaultsProvider;

	private Integer decimalScale = null;

	ElasticsearchBigDecimalIndexFieldTypeOptionsStep(ElasticsearchIndexFieldTypeBuildContext buildContext,
			IndexFieldTypeDefaultsProvider defaultsProvider) {
		super( buildContext, BigDecimal.class, DataTypes.SCALED_FLOAT, DefaultStringConverters.BIG_DECIMAL );
		this.defaultsProvider = defaultsProvider;
	}

	@Override
	public ElasticsearchBigDecimalIndexFieldTypeOptionsStep decimalScale(int decimalScale) {
		this.decimalScale = decimalScale;
		return thisAsS();
	}

	@Override
	protected ElasticsearchFieldCodec<BigDecimal> completeCodec(ElasticsearchIndexFieldTypeBuildContext buildContext) {
		int resolvedDecimalScale = resolveDecimalScale();

		ElasticsearchBigDecimalFieldCodec codec =
				new ElasticsearchBigDecimalFieldCodec( buildContext.getUserFacingGson(), resolvedDecimalScale );
		builder.mapping().setScalingFactor( codec.scalingFactor().doubleValue() );

		return codec;
	}

	@Override
	protected ElasticsearchBigDecimalIndexFieldTypeOptionsStep thisAsS() {
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
