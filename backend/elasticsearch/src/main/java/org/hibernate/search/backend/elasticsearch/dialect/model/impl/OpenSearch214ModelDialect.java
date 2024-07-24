/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.dialect.model.impl;

import org.hibernate.search.backend.elasticsearch.types.dsl.provider.impl.ElasticsearchIndexFieldTypeFactoryProvider;
import org.hibernate.search.backend.elasticsearch.types.dsl.provider.impl.OpenSearch214IndexFieldTypeFactoryProvider;
import org.hibernate.search.backend.elasticsearch.validation.impl.ElasticsearchPropertyMappingValidatorProvider;
import org.hibernate.search.backend.elasticsearch.validation.impl.OpenSearch2PropertyMappingValidatorProvider;

import com.google.gson.Gson;

/**
 * The model dialect for OpenSearch 2.14+.
 */
public class OpenSearch214ModelDialect implements ElasticsearchModelDialect {

	@Override
	public ElasticsearchIndexFieldTypeFactoryProvider createIndexTypeFieldFactoryProvider(Gson userFacingGson) {
		return new OpenSearch214IndexFieldTypeFactoryProvider( userFacingGson );
	}

	@Override
	public ElasticsearchPropertyMappingValidatorProvider createElasticsearchPropertyMappingValidatorProvider() {
		return new OpenSearch2PropertyMappingValidatorProvider();
	}
}
