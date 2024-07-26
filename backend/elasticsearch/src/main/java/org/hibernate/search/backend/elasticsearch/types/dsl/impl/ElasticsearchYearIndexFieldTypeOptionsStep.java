/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import java.time.Year;
import java.time.format.DateTimeFormatter;

import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchYearFieldCodec;
import org.hibernate.search.engine.backend.types.converter.spi.DefaultStringConverters;

import com.google.gson.Gson;

class ElasticsearchYearIndexFieldTypeOptionsStep
		extends
		AbstractElasticsearchTemporalIndexFieldTypeOptionsStep<ElasticsearchYearIndexFieldTypeOptionsStep, Year> {

	ElasticsearchYearIndexFieldTypeOptionsStep(ElasticsearchIndexFieldTypeBuildContext buildContext) {
		super( buildContext, Year.class, DefaultStringConverters.YEAR );
	}

	@Override
	protected ElasticsearchFieldCodec<Year> createCodec(Gson gson, DateTimeFormatter formatter) {
		return new ElasticsearchYearFieldCodec( gson, formatter );
	}

	@Override
	protected ElasticsearchYearIndexFieldTypeOptionsStep thisAsS() {
		return this;
	}

	@Override
	protected boolean sumAggregationSupported() {
		return false;
	}
}
