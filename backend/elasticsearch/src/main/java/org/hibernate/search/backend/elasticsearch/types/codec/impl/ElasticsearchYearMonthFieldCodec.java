/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.types.codec.impl;

import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class ElasticsearchYearMonthFieldCodec extends AbstractElasticsearchJavaTimeFieldCodec<YearMonth> {

	public ElasticsearchYearMonthFieldCodec(DateTimeFormatter delegate) {
		super( delegate );
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
