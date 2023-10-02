/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.dialect.model.impl;

import org.hibernate.search.backend.elasticsearch.types.dsl.provider.impl.Elasticsearch812IndexFieldTypeFactoryProvider;
import org.hibernate.search.backend.elasticsearch.types.dsl.provider.impl.ElasticsearchIndexFieldTypeFactoryProvider;
import org.hibernate.search.backend.elasticsearch.validation.impl.Elasticsearch812PropertyMappingValidatorProvider;
import org.hibernate.search.backend.elasticsearch.validation.impl.ElasticsearchPropertyMappingValidatorProvider;

import com.google.gson.Gson;

/**
 * The model dialect for Elasticsearch 8.12+.
 */
public class Elasticsearch812ModelDialect implements ElasticsearchModelDialect {

	@Override
	public ElasticsearchIndexFieldTypeFactoryProvider createIndexTypeFieldFactoryProvider(Gson userFacingGson) {
		return new Elasticsearch812IndexFieldTypeFactoryProvider( userFacingGson );
	}

	@Override
	public ElasticsearchPropertyMappingValidatorProvider createElasticsearchPropertyMappingValidatorProvider() {
		return new Elasticsearch812PropertyMappingValidatorProvider();
	}
}
