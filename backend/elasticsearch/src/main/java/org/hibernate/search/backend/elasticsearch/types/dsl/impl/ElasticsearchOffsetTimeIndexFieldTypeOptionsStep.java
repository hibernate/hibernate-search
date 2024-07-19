/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import java.time.OffsetTime;
import java.time.format.DateTimeFormatter;

import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchOffsetTimeFieldCodec;
import org.hibernate.search.engine.backend.types.converter.spi.DefaultStringConverters;

import com.google.gson.Gson;

class ElasticsearchOffsetTimeIndexFieldTypeOptionsStep
		extends
		AbstractElasticsearchTemporalIndexFieldTypeOptionsStep<ElasticsearchOffsetTimeIndexFieldTypeOptionsStep, OffsetTime> {

	ElasticsearchOffsetTimeIndexFieldTypeOptionsStep(ElasticsearchIndexFieldTypeBuildContext buildContext) {
		super( buildContext, OffsetTime.class, DefaultStringConverters.OFFSET_TIME );
	}

	@Override
	protected ElasticsearchFieldCodec<OffsetTime> createCodec(Gson gson, DateTimeFormatter formatter) {
		return new ElasticsearchOffsetTimeFieldCodec( gson, formatter );
	}

	@Override
	protected ElasticsearchOffsetTimeIndexFieldTypeOptionsStep thisAsS() {
		return this;
	}
}
