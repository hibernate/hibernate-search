/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import java.time.MonthDay;
import java.time.format.DateTimeFormatter;

import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchMonthDayFieldCodec;
import org.hibernate.search.engine.backend.types.converter.spi.DefaultStringConverters;

import com.google.gson.Gson;

class ElasticsearchMonthDayIndexFieldTypeOptionsStep
		extends
		AbstractElasticsearchTemporalIndexFieldTypeOptionsStep<ElasticsearchMonthDayIndexFieldTypeOptionsStep, MonthDay> {

	ElasticsearchMonthDayIndexFieldTypeOptionsStep(ElasticsearchIndexFieldTypeBuildContext buildContext) {
		super( buildContext, MonthDay.class, DefaultStringConverters.MONTH_DAY );
	}

	@Override
	protected ElasticsearchFieldCodec<MonthDay> createCodec(Gson gson, DateTimeFormatter formatter) {
		return new ElasticsearchMonthDayFieldCodec( gson, formatter );
	}

	@Override
	protected ElasticsearchMonthDayIndexFieldTypeOptionsStep thisAsS() {
		return this;
	}
}
