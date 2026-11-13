package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import org.hibernate.search.backend.elasticsearch.json.internal.spi.JsonCodecService;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchLocalTimeFieldCodec;
import org.hibernate.search.engine.backend.types.converter.spi.DefaultStringConverters;

class ElasticsearchLocalTimeIndexFieldTypeOptionsStep
		extends
		AbstractElasticsearchTemporalIndexFieldTypeOptionsStep<ElasticsearchLocalTimeIndexFieldTypeOptionsStep, LocalTime> {

	ElasticsearchLocalTimeIndexFieldTypeOptionsStep(ElasticsearchIndexFieldTypeBuildContext buildContext) {
		super( buildContext, LocalTime.class, DefaultStringConverters.LOCAL_TIME );
	}

	@Override
	protected ElasticsearchFieldCodec<LocalTime> createCodec(JsonCodecService json, DateTimeFormatter formatter) {
		return new ElasticsearchLocalTimeFieldCodec( json, formatter );
	}

	@Override
	protected ElasticsearchLocalTimeIndexFieldTypeOptionsStep thisAsS() {
		return this;
	}
}
