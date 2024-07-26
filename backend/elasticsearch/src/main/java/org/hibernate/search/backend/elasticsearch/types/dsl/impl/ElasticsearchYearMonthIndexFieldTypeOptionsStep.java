/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchYearMonthFieldCodec;
import org.hibernate.search.engine.backend.types.converter.spi.DefaultStringConverters;

import com.google.gson.Gson;

class ElasticsearchYearMonthIndexFieldTypeOptionsStep
		extends
		AbstractElasticsearchTemporalIndexFieldTypeOptionsStep<ElasticsearchYearMonthIndexFieldTypeOptionsStep, YearMonth> {

	ElasticsearchYearMonthIndexFieldTypeOptionsStep(ElasticsearchIndexFieldTypeBuildContext buildContext) {
		super( buildContext, YearMonth.class, DefaultStringConverters.YEAR_MONTH );
	}

	@Override
	protected ElasticsearchFieldCodec<YearMonth> createCodec(Gson gson, DateTimeFormatter formatter) {
		return new ElasticsearchYearMonthFieldCodec( gson, formatter );
	}

	@Override
	protected ElasticsearchYearMonthIndexFieldTypeOptionsStep thisAsS() {
		return this;
	}

	@Override
	protected boolean sumAggregationSupported() {
		return false;
	}
}
