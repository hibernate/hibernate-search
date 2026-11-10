package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

import org.hibernate.search.backend.elasticsearch.json.internal.spi.JsonCodecService;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchInstantFieldCodec;
import org.hibernate.search.engine.backend.types.converter.spi.DefaultStringConverters;

class ElasticsearchInstantIndexFieldTypeOptionsStep
		extends
		AbstractElasticsearchTemporalIndexFieldTypeOptionsStep<ElasticsearchInstantIndexFieldTypeOptionsStep, Instant> {

	ElasticsearchInstantIndexFieldTypeOptionsStep(ElasticsearchIndexFieldTypeBuildContext buildContext) {
		super( buildContext, Instant.class, DefaultStringConverters.INSTANT );
	}

	@Override
	protected ElasticsearchFieldCodec<Instant> createCodec(JsonCodecService json, DateTimeFormatter formatter) {
		return new ElasticsearchInstantFieldCodec( json, formatter );
	}

	@Override
	protected ElasticsearchInstantIndexFieldTypeOptionsStep thisAsS() {
		return this;
	}
}
