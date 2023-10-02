/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import java.time.MonthDay;
import java.time.format.DateTimeFormatter;

import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchMonthDayFieldCodec;
import org.hibernate.search.engine.backend.types.converter.spi.DefaultParseConverters;

class ElasticsearchMonthDayIndexFieldTypeOptionsStep
		extends
		AbstractElasticsearchTemporalIndexFieldTypeOptionsStep<ElasticsearchMonthDayIndexFieldTypeOptionsStep, MonthDay> {

	ElasticsearchMonthDayIndexFieldTypeOptionsStep(ElasticsearchIndexFieldTypeBuildContext buildContext) {
		super( buildContext, MonthDay.class, DefaultParseConverters.MONTH_DAY );
	}

	@Override
	protected ElasticsearchFieldCodec<MonthDay> createCodec(DateTimeFormatter formatter) {
		return new ElasticsearchMonthDayFieldCodec( formatter );
	}

	@Override
	protected ElasticsearchMonthDayIndexFieldTypeOptionsStep thisAsS() {
		return this;
	}
}
