/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchYearMonthFieldCodec;

class ElasticsearchYearMonthIndexFieldTypeOptionsStep
		extends
		AbstractElasticsearchTemporalIndexFieldTypeOptionsStep<ElasticsearchYearMonthIndexFieldTypeOptionsStep, YearMonth> {

	ElasticsearchYearMonthIndexFieldTypeOptionsStep(ElasticsearchIndexFieldTypeBuildContext buildContext) {
		super( buildContext, YearMonth.class );
	}

	@Override
	protected ElasticsearchFieldCodec<YearMonth> createCodec(DateTimeFormatter formatter) {
		return new ElasticsearchYearMonthFieldCodec( formatter );
	}

	@Override
	protected ElasticsearchYearMonthIndexFieldTypeOptionsStep thisAsS() {
		return this;
	}
}
