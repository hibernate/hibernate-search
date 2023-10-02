/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.dialect.model.impl;

import org.hibernate.search.backend.elasticsearch.types.dsl.provider.impl.ElasticsearchIndexFieldTypeFactoryProvider;
import org.hibernate.search.backend.elasticsearch.types.dsl.provider.impl.OpenSearch1IndexFieldTypeFactoryProvider;
import org.hibernate.search.backend.elasticsearch.validation.impl.ElasticsearchPropertyMappingValidatorProvider;
import org.hibernate.search.backend.elasticsearch.validation.impl.OpenSearch1PropertyMappingValidatorProvider;

import com.google.gson.Gson;

/**
 * The model dialect for OpenSearch [2.0-2.9).
 */
public class OpenSearch2ModelDialect implements ElasticsearchModelDialect {

	@Override
	public ElasticsearchIndexFieldTypeFactoryProvider createIndexTypeFieldFactoryProvider(Gson userFacingGson) {
		return new OpenSearch1IndexFieldTypeFactoryProvider( userFacingGson );
	}

	@Override
	public ElasticsearchPropertyMappingValidatorProvider createElasticsearchPropertyMappingValidatorProvider() {
		return new OpenSearch1PropertyMappingValidatorProvider();
	}
}
