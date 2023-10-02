/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.DataTypes;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFloatFieldCodec;
import org.hibernate.search.engine.backend.types.converter.spi.DefaultParseConverters;

class ElasticsearchFloatIndexFieldTypeOptionsStep
		extends AbstractElasticsearchNumericFieldTypeOptionsStep<ElasticsearchFloatIndexFieldTypeOptionsStep, Float> {

	ElasticsearchFloatIndexFieldTypeOptionsStep(ElasticsearchIndexFieldTypeBuildContext buildContext) {
		super( buildContext, Float.class, DataTypes.FLOAT, DefaultParseConverters.FLOAT );
	}

	@Override
	protected ElasticsearchFieldCodec<Float> completeCodec() {
		return ElasticsearchFloatFieldCodec.INSTANCE;
	}

	@Override
	protected ElasticsearchFloatIndexFieldTypeOptionsStep thisAsS() {
		return this;
	}
}
