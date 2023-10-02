/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.types.codec.impl;

import java.time.LocalDate;
import java.time.Month;
import java.time.OffsetTime;
import java.time.format.DateTimeFormatter;

public class ElasticsearchOffsetTimeFieldCodec extends AbstractElasticsearchJavaTimeFieldCodec<OffsetTime> {

	private static final LocalDate EPOCH_DATE = LocalDate.of( 1970, Month.JANUARY, 1 );

	public ElasticsearchOffsetTimeFieldCodec(DateTimeFormatter delegate) {
		super( delegate );
	}

	@Override
	protected OffsetTime nullUnsafeParse(String stringValue) {
		return OffsetTime.parse( stringValue, formatter );
	}

	@Override
	protected Long nullUnsafeScalar(OffsetTime value) {
		return value.atDate( EPOCH_DATE ).toInstant().toEpochMilli();
	}
}
