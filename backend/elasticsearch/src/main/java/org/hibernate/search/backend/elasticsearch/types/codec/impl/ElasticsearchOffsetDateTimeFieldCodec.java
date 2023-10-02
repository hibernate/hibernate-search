/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.types.codec.impl;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

public class ElasticsearchOffsetDateTimeFieldCodec extends AbstractElasticsearchJavaTimeFieldCodec<OffsetDateTime> {

	public ElasticsearchOffsetDateTimeFieldCodec(DateTimeFormatter delegate) {
		super( delegate );
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
