/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.dialect.model.impl;

import org.hibernate.search.backend.elasticsearch.types.dsl.provider.impl.Elasticsearch814IndexFieldTypeFactoryProvider;
import org.hibernate.search.backend.elasticsearch.types.dsl.provider.impl.ElasticsearchIndexFieldTypeFactoryProvider;
import org.hibernate.search.backend.elasticsearch.validation.impl.Elasticsearch814PropertyMappingValidatorProvider;
import org.hibernate.search.backend.elasticsearch.validation.impl.ElasticsearchPropertyMappingValidatorProvider;

import com.google.gson.Gson;

/**
 * The model dialect for Elasticsearch 8.14+.
 */
public class Elasticsearch814ModelDialect implements ElasticsearchModelDialect {

	@Override
	public ElasticsearchIndexFieldTypeFactoryProvider createIndexTypeFieldFactoryProvider(Gson userFacingGson) {
		return new Elasticsearch814IndexFieldTypeFactoryProvider( userFacingGson );
	}

	@Override
	public ElasticsearchPropertyMappingValidatorProvider createElasticsearchPropertyMappingValidatorProvider() {
		return new Elasticsearch814PropertyMappingValidatorProvider();
	}
}
