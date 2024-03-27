/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchInstantFieldCodec;

class ElasticsearchInstantIndexFieldTypeOptionsStep
		extends
		AbstractElasticsearchTemporalIndexFieldTypeOptionsStep<ElasticsearchInstantIndexFieldTypeOptionsStep, Instant> {

	ElasticsearchInstantIndexFieldTypeOptionsStep(ElasticsearchIndexFieldTypeBuildContext buildContext) {
		super( buildContext, Instant.class );
	}

	@Override
	protected ElasticsearchFieldCodec<Instant> createCodec(DateTimeFormatter formatter) {
		return new ElasticsearchInstantFieldCodec( formatter );
	}

	@Override
	protected ElasticsearchInstantIndexFieldTypeOptionsStep thisAsS() {
		return this;
	}
}
