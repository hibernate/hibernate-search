/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchZonedDateTimeFieldCodec;
import org.hibernate.search.engine.backend.types.converter.spi.DefaultStringConverters;

import com.google.gson.Gson;

class ElasticsearchZonedDateTimeIndexFieldTypeOptionsStep
		extends
		AbstractElasticsearchTemporalIndexFieldTypeOptionsStep<ElasticsearchZonedDateTimeIndexFieldTypeOptionsStep,
				ZonedDateTime> {

	ElasticsearchZonedDateTimeIndexFieldTypeOptionsStep(ElasticsearchIndexFieldTypeBuildContext buildContext) {
		super( buildContext, ZonedDateTime.class, DefaultStringConverters.ZONED_DATE_TIME );
	}

	@Override
	protected ElasticsearchFieldCodec<ZonedDateTime> createCodec(Gson gson, DateTimeFormatter formatter) {
		return new ElasticsearchZonedDateTimeFieldCodec( gson, formatter );
	}

	@Override
	protected ElasticsearchZonedDateTimeIndexFieldTypeOptionsStep thisAsS() {
		return this;
	}
}
