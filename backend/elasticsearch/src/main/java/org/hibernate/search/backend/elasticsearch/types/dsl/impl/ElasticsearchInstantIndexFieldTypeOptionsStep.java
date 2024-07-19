/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchInstantFieldCodec;
import org.hibernate.search.engine.backend.types.converter.spi.DefaultStringConverters;

import com.google.gson.Gson;

class ElasticsearchInstantIndexFieldTypeOptionsStep
		extends
		AbstractElasticsearchTemporalIndexFieldTypeOptionsStep<ElasticsearchInstantIndexFieldTypeOptionsStep, Instant> {

	ElasticsearchInstantIndexFieldTypeOptionsStep(ElasticsearchIndexFieldTypeBuildContext buildContext) {
		super( buildContext, Instant.class, DefaultStringConverters.INSTANT );
	}

	@Override
	protected ElasticsearchFieldCodec<Instant> createCodec(Gson gson, DateTimeFormatter formatter) {
		return new ElasticsearchInstantFieldCodec( gson, formatter );
	}

	@Override
	protected ElasticsearchInstantIndexFieldTypeOptionsStep thisAsS() {
		return this;
	}
}
