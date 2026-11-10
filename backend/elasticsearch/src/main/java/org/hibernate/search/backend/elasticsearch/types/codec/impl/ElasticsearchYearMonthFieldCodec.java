package org.hibernate.search.backend.elasticsearch.types.codec.impl;

import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import org.hibernate.search.backend.elasticsearch.json.internal.spi.JsonCodecService;

public class ElasticsearchYearMonthFieldCodec extends AbstractElasticsearchJavaTimeFieldCodec<YearMonth> {

	public ElasticsearchYearMonthFieldCodec(JsonCodecService json, DateTimeFormatter delegate) {
		super( json, delegate );
	}

	@Override
	protected YearMonth nullUnsafeParse(String stringValue) {
		return YearMonth.parse( stringValue, formatter );
	}

	@Override
	protected Long nullUnsafeScalar(YearMonth value) {
		return value.atDay( 1 ).atStartOfDay().toInstant( ZoneOffset.UTC ).toEpochMilli();
	}
}
