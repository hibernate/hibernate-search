/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchLocalDateFieldCodec;

class ElasticsearchLocalDateIndexFieldTypeOptionsStep
		extends
		AbstractElasticsearchTemporalIndexFieldTypeOptionsStep<ElasticsearchLocalDateIndexFieldTypeOptionsStep, LocalDate> {

	ElasticsearchLocalDateIndexFieldTypeOptionsStep(ElasticsearchIndexFieldTypeBuildContext buildContext) {
		super( buildContext, LocalDate.class );
	}

	@Override
	protected ElasticsearchFieldCodec<LocalDate> createCodec(DateTimeFormatter formatter) {
		return new ElasticsearchLocalDateFieldCodec( formatter );
	}

	@Override
	protected ElasticsearchLocalDateIndexFieldTypeOptionsStep thisAsS() {
		return this;
	}
}
