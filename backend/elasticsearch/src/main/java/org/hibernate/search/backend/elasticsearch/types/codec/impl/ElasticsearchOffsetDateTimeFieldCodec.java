package org.hibernate.search.backend.elasticsearch.types.codec.impl;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

import org.hibernate.search.backend.elasticsearch.json.internal.spi.JsonCodecService;

public class ElasticsearchOffsetDateTimeFieldCodec extends AbstractElasticsearchJavaTimeFieldCodec<OffsetDateTime> {

	public ElasticsearchOffsetDateTimeFieldCodec(JsonCodecService json, DateTimeFormatter delegate) {
		super( json, delegate );
	}

	@Override
	protected OffsetDateTime nullUnsafeParse(String stringValue) {
		return OffsetDateTime.parse( stringValue, formatter );
	}

	@Override
	protected Long nullUnsafeScalar(OffsetDateTime value) {
		return value.toInstant().toEpochMilli();
	}
}
