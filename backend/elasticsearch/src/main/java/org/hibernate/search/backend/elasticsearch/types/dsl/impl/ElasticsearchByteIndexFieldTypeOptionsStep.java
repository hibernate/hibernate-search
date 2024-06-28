/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.DataTypes;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchByteFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.engine.backend.types.converter.spi.DefaultStringConverters;

class ElasticsearchByteIndexFieldTypeOptionsStep
		extends AbstractElasticsearchNumericFieldTypeOptionsStep<ElasticsearchByteIndexFieldTypeOptionsStep, Byte> {

	ElasticsearchByteIndexFieldTypeOptionsStep(ElasticsearchIndexFieldTypeBuildContext buildContext) {
		super( buildContext, Byte.class, DataTypes.BYTE, DefaultStringConverters.BYTE );
	}

	@Override
	protected ElasticsearchFieldCodec<Byte> completeCodec() {
		return ElasticsearchByteFieldCodec.INSTANCE;
	}

	@Override
	protected ElasticsearchByteIndexFieldTypeOptionsStep thisAsS() {
		return this;
	}
}
