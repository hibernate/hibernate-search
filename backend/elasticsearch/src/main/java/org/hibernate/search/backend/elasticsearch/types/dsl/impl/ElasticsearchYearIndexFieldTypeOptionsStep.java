/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import java.time.Year;
import java.time.format.DateTimeFormatter;

import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchYearFieldCodec;

class ElasticsearchYearIndexFieldTypeOptionsStep
		extends
		AbstractElasticsearchTemporalIndexFieldTypeOptionsStep<ElasticsearchYearIndexFieldTypeOptionsStep, Year> {

	ElasticsearchYearIndexFieldTypeOptionsStep(ElasticsearchIndexFieldTypeBuildContext buildContext) {
		super( buildContext, Year.class );
	}

	@Override
	protected ElasticsearchFieldCodec<Year> createCodec(DateTimeFormatter formatter) {
		return new ElasticsearchYearFieldCodec( formatter );
	}

	@Override
	protected ElasticsearchYearIndexFieldTypeOptionsStep thisAsS() {
		return this;
	}
}
