/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.types.codec.impl;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class ElasticsearchLocalTimeFieldCodec extends AbstractElasticsearchJavaTimeFieldCodec<LocalTime> {

	private static final LocalDate EPOCH_DAY = LocalDate.of( 1970, Month.JANUARY, 1 );

	public ElasticsearchLocalTimeFieldCodec(DateTimeFormatter delegate) {
		super( delegate );
	}

	@Override
	protected LocalTime nullUnsafeParse(String stringValue) {
		return LocalTime.parse( stringValue, formatter );
	}

	@Override
	protected Long nullUnsafeScalar(LocalTime value) {
		return value.atDate( EPOCH_DAY ).toInstant( ZoneOffset.UTC ).toEpochMilli();
	}
}
