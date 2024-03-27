/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.DataTypes;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchLongFieldCodec;

class ElasticsearchLongIndexFieldTypeOptionsStep
		extends AbstractElasticsearchNumericFieldTypeOptionsStep<ElasticsearchLongIndexFieldTypeOptionsStep, Long> {

	ElasticsearchLongIndexFieldTypeOptionsStep(ElasticsearchIndexFieldTypeBuildContext buildContext) {
		super( buildContext, Long.class, DataTypes.LONG );
	}

	@Override
	protected ElasticsearchFieldCodec<Long> completeCodec() {
		return ElasticsearchLongFieldCodec.INSTANCE;
	}

	@Override
	protected ElasticsearchLongIndexFieldTypeOptionsStep thisAsS() {
		return this;
	}
}
