package org.hibernate.search.backend.elasticsearch.types.codec.impl;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import org.hibernate.search.backend.elasticsearch.json.internal.spi.JsonCodecService;

public class ElasticsearchLocalDateTimeFieldCodec extends AbstractElasticsearchJavaTimeFieldCodec<LocalDateTime> {

	public ElasticsearchLocalDateTimeFieldCodec(JsonCodecService json, DateTimeFormatter delegate) {
		super( json, delegate );
	}

	@Override
	protected LocalDateTime nullUnsafeParse(String stringValue) {
		return LocalDateTime.parse( stringValue, formatter );
	}

	@Override
	protected Long nullUnsafeScalar(LocalDateTime value) {
		return value.toInstant( ZoneOffset.UTC ).toEpochMilli();
	}
}
