/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.types.codec.impl;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

import com.google.gson.Gson;

public class ElasticsearchOffsetDateTimeFieldCodec extends AbstractElasticsearchJavaTimeFieldCodec<OffsetDateTime> {

	public ElasticsearchOffsetDateTimeFieldCodec(Gson gson, DateTimeFormatter delegate) {
		super( gson, delegate );
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
