/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchOffsetDateTimeFieldCodec;
import org.hibernate.search.engine.backend.types.converter.spi.DefaultStringConverters;

import com.google.gson.Gson;

class ElasticsearchOffsetDateTimeIndexFieldTypeOptionsStep
		extends
		AbstractElasticsearchTemporalIndexFieldTypeOptionsStep<ElasticsearchOffsetDateTimeIndexFieldTypeOptionsStep,
				OffsetDateTime> {

	ElasticsearchOffsetDateTimeIndexFieldTypeOptionsStep(ElasticsearchIndexFieldTypeBuildContext buildContext) {
		super( buildContext, OffsetDateTime.class, DefaultStringConverters.OFFSET_DATE_TIME );
	}

	@Override
	protected ElasticsearchFieldCodec<OffsetDateTime> createCodec(Gson gson, DateTimeFormatter formatter) {
		return new ElasticsearchOffsetDateTimeFieldCodec( gson, formatter );
	}

	@Override
	protected ElasticsearchOffsetDateTimeIndexFieldTypeOptionsStep thisAsS() {
		return this;
	}
}
