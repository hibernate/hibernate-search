/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.types.codec.impl;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import com.google.gson.Gson;

public class ElasticsearchLocalDateTimeFieldCodec extends AbstractElasticsearchJavaTimeFieldCodec<LocalDateTime> {

	public ElasticsearchLocalDateTimeFieldCodec(Gson gson, DateTimeFormatter delegate) {
		super( gson, delegate );
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
