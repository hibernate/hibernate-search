/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.types.codec.impl;

import java.time.Year;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class ElasticsearchYearFieldCodec extends AbstractElasticsearchJavaTimeFieldCodec<Year> {

	public ElasticsearchYearFieldCodec(DateTimeFormatter delegate) {
		super( delegate );
	}

	@Override
	protected Year nullUnsafeParse(String stringValue) {
		return Year.parse( stringValue, formatter );
	}

	@Override
	protected Long nullUnsafeScalar(Year value) {
		return value.atDay( 1 ).atStartOfDay().toInstant( ZoneOffset.UTC ).toEpochMilli();
	}
}
