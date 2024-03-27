/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.DataTypes;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchShortFieldCodec;

class ElasticsearchShortIndexFieldTypeOptionsStep
		extends AbstractElasticsearchNumericFieldTypeOptionsStep<ElasticsearchShortIndexFieldTypeOptionsStep, Short> {

	ElasticsearchShortIndexFieldTypeOptionsStep(ElasticsearchIndexFieldTypeBuildContext buildContext) {
		super( buildContext, Short.class, DataTypes.SHORT );
	}

	@Override
	protected ElasticsearchFieldCodec<Short> completeCodec() {
		return ElasticsearchShortFieldCodec.INSTANCE;
	}

	@Override
	protected ElasticsearchShortIndexFieldTypeOptionsStep thisAsS() {
		return this;
	}
}
