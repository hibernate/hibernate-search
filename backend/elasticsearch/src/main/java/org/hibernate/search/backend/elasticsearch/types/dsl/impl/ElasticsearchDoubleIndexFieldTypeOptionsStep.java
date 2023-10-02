/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.DataTypes;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchDoubleFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.engine.backend.types.converter.spi.DefaultParseConverters;

class ElasticsearchDoubleIndexFieldTypeOptionsStep
		extends AbstractElasticsearchNumericFieldTypeOptionsStep<ElasticsearchDoubleIndexFieldTypeOptionsStep, Double> {

	ElasticsearchDoubleIndexFieldTypeOptionsStep(ElasticsearchIndexFieldTypeBuildContext buildContext) {
		super( buildContext, Double.class, DataTypes.DOUBLE, DefaultParseConverters.DOUBLE );
	}

	@Override
	protected ElasticsearchFieldCodec<Double> completeCodec() {
		return ElasticsearchDoubleFieldCodec.INSTANCE;
	}

	@Override
	protected ElasticsearchDoubleIndexFieldTypeOptionsStep thisAsS() {
		return this;
	}
}
