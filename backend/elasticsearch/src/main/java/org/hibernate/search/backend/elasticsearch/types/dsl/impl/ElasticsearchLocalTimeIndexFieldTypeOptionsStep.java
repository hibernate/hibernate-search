/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchLocalTimeFieldCodec;
import org.hibernate.search.engine.backend.types.converter.spi.DefaultStringConverters;

import com.google.gson.Gson;

class ElasticsearchLocalTimeIndexFieldTypeOptionsStep
		extends
		AbstractElasticsearchTemporalIndexFieldTypeOptionsStep<ElasticsearchLocalTimeIndexFieldTypeOptionsStep, LocalTime> {

	ElasticsearchLocalTimeIndexFieldTypeOptionsStep(ElasticsearchIndexFieldTypeBuildContext buildContext) {
		super( buildContext, LocalTime.class, DefaultStringConverters.LOCAL_TIME );
	}

	@Override
	protected ElasticsearchFieldCodec<LocalTime> createCodec(Gson gson, DateTimeFormatter formatter) {
		return new ElasticsearchLocalTimeFieldCodec( gson, formatter );
	}

	@Override
	protected ElasticsearchLocalTimeIndexFieldTypeOptionsStep thisAsS() {
		return this;
	}
}
