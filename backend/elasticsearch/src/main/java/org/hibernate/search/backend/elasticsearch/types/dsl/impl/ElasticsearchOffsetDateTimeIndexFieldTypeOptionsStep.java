package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

import org.hibernate.search.backend.elasticsearch.json.internal.spi.JsonCodecService;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchOffsetDateTimeFieldCodec;
import org.hibernate.search.engine.backend.types.converter.spi.DefaultStringConverters;

class ElasticsearchOffsetDateTimeIndexFieldTypeOptionsStep
		extends
		AbstractElasticsearchTemporalIndexFieldTypeOptionsStep<ElasticsearchOffsetDateTimeIndexFieldTypeOptionsStep,
				OffsetDateTime> {

	ElasticsearchOffsetDateTimeIndexFieldTypeOptionsStep(ElasticsearchIndexFieldTypeBuildContext buildContext) {
		super( buildContext, OffsetDateTime.class, DefaultStringConverters.OFFSET_DATE_TIME );
	}

	@Override
	protected ElasticsearchFieldCodec<OffsetDateTime> createCodec(JsonCodecService json, DateTimeFormatter formatter) {
		return new ElasticsearchOffsetDateTimeFieldCodec( json, formatter );
	}

	@Override
	protected ElasticsearchOffsetDateTimeIndexFieldTypeOptionsStep thisAsS() {
		return this;
	}
}
