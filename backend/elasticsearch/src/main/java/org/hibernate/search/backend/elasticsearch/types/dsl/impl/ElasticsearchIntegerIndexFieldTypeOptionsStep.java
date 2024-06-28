/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.DataTypes;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchIntegerFieldCodec;
import org.hibernate.search.engine.backend.types.converter.spi.DefaultStringConverters;

class ElasticsearchIntegerIndexFieldTypeOptionsStep
		extends
		AbstractElasticsearchNumericFieldTypeOptionsStep<ElasticsearchIntegerIndexFieldTypeOptionsStep, Integer> {

	ElasticsearchIntegerIndexFieldTypeOptionsStep(ElasticsearchIndexFieldTypeBuildContext buildContext) {
		super( buildContext, Integer.class, DataTypes.INTEGER, DefaultStringConverters.INTEGER );
	}

	@Override
	protected ElasticsearchFieldCodec<Integer> completeCodec() {
		return ElasticsearchIntegerFieldCodec.INSTANCE;
	}

	@Override
	protected ElasticsearchIntegerIndexFieldTypeOptionsStep thisAsS() {
		return this;
	}
}
