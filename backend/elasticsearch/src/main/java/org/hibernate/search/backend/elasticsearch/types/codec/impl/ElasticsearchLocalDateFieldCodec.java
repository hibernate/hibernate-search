/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.types.codec.impl;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class ElasticsearchLocalDateFieldCodec extends AbstractElasticsearchJavaTimeFieldCodec<LocalDate> {

	public ElasticsearchLocalDateFieldCodec(DateTimeFormatter delegate) {
		super( delegate );
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
