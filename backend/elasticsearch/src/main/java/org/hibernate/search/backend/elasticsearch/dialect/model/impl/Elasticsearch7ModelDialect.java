/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.dialect.model.impl;

import org.hibernate.search.backend.elasticsearch.types.dsl.provider.impl.Elasticsearch7IndexFieldTypeFactoryProvider;
import org.hibernate.search.backend.elasticsearch.types.dsl.provider.impl.ElasticsearchIndexFieldTypeFactoryProvider;
import org.hibernate.search.backend.elasticsearch.validation.impl.Elasticsearch7PropertyMappingValidatorProvider;
import org.hibernate.search.backend.elasticsearch.validation.impl.ElasticsearchPropertyMappingValidatorProvider;

import com.google.gson.Gson;

/**
 * The model dialect for Elasticsearch 7.x.
 */
public class Elasticsearch7ModelDialect implements ElasticsearchModelDialect {

	@Override
	public ElasticsearchIndexFieldTypeFactoryProvider createIndexTypeFieldFactoryProvider(Gson userFacingGson) {
		return new Elasticsearch7IndexFieldTypeFactoryProvider( userFacingGson );
	}

	@Override
	public ElasticsearchPropertyMappingValidatorProvider createElasticsearchPropertyMappingValidatorProvider() {
		return new Elasticsearch7PropertyMappingValidatorProvider();
	}
}
