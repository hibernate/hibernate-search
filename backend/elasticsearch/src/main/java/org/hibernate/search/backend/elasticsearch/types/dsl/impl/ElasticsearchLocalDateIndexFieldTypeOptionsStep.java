package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.hibernate.search.backend.elasticsearch.json.internal.spi.JsonCodecService;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchLocalDateFieldCodec;
import org.hibernate.search.engine.backend.types.converter.spi.DefaultStringConverters;

class ElasticsearchLocalDateIndexFieldTypeOptionsStep
		extends
		AbstractElasticsearchTemporalIndexFieldTypeOptionsStep<ElasticsearchLocalDateIndexFieldTypeOptionsStep, LocalDate> {

	ElasticsearchLocalDateIndexFieldTypeOptionsStep(ElasticsearchIndexFieldTypeBuildContext buildContext) {
		super( buildContext, LocalDate.class, DefaultStringConverters.LOCAL_DATE );
	}

	@Override
	protected ElasticsearchFieldCodec<LocalDate> createCodec(JsonCodecService json, DateTimeFormatter formatter) {
		return new ElasticsearchLocalDateFieldCodec( json, formatter );
	}

	@Override
	protected ElasticsearchLocalDateIndexFieldTypeOptionsStep thisAsS() {
		return this;
	}
}
