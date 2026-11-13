package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import java.time.OffsetTime;
import java.time.format.DateTimeFormatter;

import org.hibernate.search.backend.elasticsearch.json.internal.spi.JsonCodecService;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchOffsetTimeFieldCodec;
import org.hibernate.search.engine.backend.types.converter.spi.DefaultStringConverters;

class ElasticsearchOffsetTimeIndexFieldTypeOptionsStep
		extends
		AbstractElasticsearchTemporalIndexFieldTypeOptionsStep<ElasticsearchOffsetTimeIndexFieldTypeOptionsStep, OffsetTime> {

	ElasticsearchOffsetTimeIndexFieldTypeOptionsStep(ElasticsearchIndexFieldTypeBuildContext buildContext) {
		super( buildContext, OffsetTime.class, DefaultStringConverters.OFFSET_TIME );
	}

	@Override
	protected ElasticsearchFieldCodec<OffsetTime> createCodec(JsonCodecService json, DateTimeFormatter formatter) {
		return new ElasticsearchOffsetTimeFieldCodec( json, formatter );
	}

	@Override
	protected ElasticsearchOffsetTimeIndexFieldTypeOptionsStep thisAsS() {
		return this;
	}
}
