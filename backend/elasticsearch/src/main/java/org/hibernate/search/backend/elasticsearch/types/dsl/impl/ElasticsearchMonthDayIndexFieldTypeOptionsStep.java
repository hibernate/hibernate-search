package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import java.time.MonthDay;
import java.time.format.DateTimeFormatter;

import org.hibernate.search.backend.elasticsearch.json.internal.spi.JsonCodecService;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchMonthDayFieldCodec;
import org.hibernate.search.engine.backend.types.converter.spi.DefaultStringConverters;

class ElasticsearchMonthDayIndexFieldTypeOptionsStep
		extends
		AbstractElasticsearchTemporalIndexFieldTypeOptionsStep<ElasticsearchMonthDayIndexFieldTypeOptionsStep, MonthDay> {

	ElasticsearchMonthDayIndexFieldTypeOptionsStep(ElasticsearchIndexFieldTypeBuildContext buildContext) {
		super( buildContext, MonthDay.class, DefaultStringConverters.MONTH_DAY );
	}

	@Override
	protected ElasticsearchFieldCodec<MonthDay> createCodec(JsonCodecService json, DateTimeFormatter formatter) {
		return new ElasticsearchMonthDayFieldCodec( json, formatter );
	}

	@Override
	protected ElasticsearchMonthDayIndexFieldTypeOptionsStep thisAsS() {
		return this;
	}
}
