package org.hibernate.search.backend.elasticsearch.types.codec.impl;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import org.hibernate.search.backend.elasticsearch.json.internal.spi.JsonCodecService;

public class ElasticsearchLocalDateFieldCodec extends AbstractElasticsearchJavaTimeFieldCodec<LocalDate> {

	public ElasticsearchLocalDateFieldCodec(JsonCodecService json, DateTimeFormatter delegate) {
		super( json, delegate );
	}

	@Override
	protected LocalDate nullUnsafeParse(String stringValue) {
		return LocalDate.parse( stringValue, formatter );
	}

	@Override
	protected Long nullUnsafeScalar(LocalDate value) {
		return value.atStartOfDay( ZoneOffset.UTC ).toInstant().toEpochMilli();
	}
}
