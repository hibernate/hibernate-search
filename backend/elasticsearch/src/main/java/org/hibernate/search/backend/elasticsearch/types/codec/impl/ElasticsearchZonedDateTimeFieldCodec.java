/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.types.codec.impl;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.hibernate.search.util.common.impl.TimeHelper;

public class ElasticsearchZonedDateTimeFieldCodec extends AbstractElasticsearchJavaTimeFieldCodec<ZonedDateTime> {

	public ElasticsearchZonedDateTimeFieldCodec(DateTimeFormatter delegate) {
		super( delegate );
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
