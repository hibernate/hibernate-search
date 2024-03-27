/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import java.time.OffsetTime;
import java.time.format.DateTimeFormatter;

import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchOffsetTimeFieldCodec;

class ElasticsearchOffsetTimeIndexFieldTypeOptionsStep
		extends
		AbstractElasticsearchTemporalIndexFieldTypeOptionsStep<ElasticsearchOffsetTimeIndexFieldTypeOptionsStep, OffsetTime> {

	ElasticsearchOffsetTimeIndexFieldTypeOptionsStep(ElasticsearchIndexFieldTypeBuildContext buildContext) {
		super( buildContext, OffsetTime.class );
	}

	@Override
	protected ElasticsearchFieldCodec<OffsetTime> createCodec(DateTimeFormatter formatter) {
		return new ElasticsearchOffsetTimeFieldCodec( formatter );
	}

	@Override
	protected ElasticsearchOffsetTimeIndexFieldTypeOptionsStep thisAsS() {
		return this;
	}
}
