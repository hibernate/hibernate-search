package org.hibernate.search.backend.elasticsearch.types.codec.impl;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.hibernate.search.backend.elasticsearch.json.internal.spi.JsonCodecService;
import org.hibernate.search.util.common.impl.TimeHelper;

public class ElasticsearchZonedDateTimeFieldCodec extends AbstractElasticsearchJavaTimeFieldCodec<ZonedDateTime> {

	public ElasticsearchZonedDateTimeFieldCodec(JsonCodecService json, DateTimeFormatter delegate) {
		super( json, delegate );
	}

	@Override
	protected ZonedDateTime nullUnsafeParse(String stringValue) {
		return TimeHelper.parseZoneDateTime( stringValue, formatter );
	}

	@Override
	protected Long nullUnsafeScalar(ZonedDateTime value) {
		return value.toInstant().toEpochMilli();
	}
}
