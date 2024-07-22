/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchLocalDateTimeFieldCodec;
import org.hibernate.search.engine.backend.types.converter.spi.DefaultStringConverters;

class ElasticsearchLocalDateTimeIndexFieldTypeOptionsStep
		extends
		AbstractElasticsearchTemporalIndexFieldTypeOptionsStep<ElasticsearchLocalDateTimeIndexFieldTypeOptionsStep,
				LocalDateTime> {

	ElasticsearchLocalDateTimeIndexFieldTypeOptionsStep(ElasticsearchIndexFieldTypeBuildContext buildContext) {
		super( buildContext, LocalDateTime.class, DefaultStringConverters.LOCAL_DATE_TIME );
	}

	@Override
	protected ElasticsearchFieldCodec<LocalDateTime> createCodec(DateTimeFormatter formatter) {
		return new ElasticsearchLocalDateTimeFieldCodec( formatter );
	}

	@Override
	protected ElasticsearchLocalDateTimeIndexFieldTypeOptionsStep thisAsS() {
		return this;
	}
}
