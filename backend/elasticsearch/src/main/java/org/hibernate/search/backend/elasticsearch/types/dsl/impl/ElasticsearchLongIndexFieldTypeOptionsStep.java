/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.DataTypes;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchLongFieldCodec;
import org.hibernate.search.engine.backend.types.converter.spi.DefaultStringConverters;

class ElasticsearchLongIndexFieldTypeOptionsStep
		extends AbstractElasticsearchNumericFieldTypeOptionsStep<ElasticsearchLongIndexFieldTypeOptionsStep, Long> {

	ElasticsearchLongIndexFieldTypeOptionsStep(ElasticsearchIndexFieldTypeBuildContext buildContext) {
		super( buildContext, Long.class, DataTypes.LONG, DefaultStringConverters.LONG );
	}

	@Override
	protected ElasticsearchFieldCodec<Long> completeCodec(ElasticsearchIndexFieldTypeBuildContext buildContext) {
		return new ElasticsearchLongFieldCodec( buildContext.getUserFacingGson() );
	}

	@Override
	protected ElasticsearchLongIndexFieldTypeOptionsStep thisAsS() {
		return this;
	}
}
