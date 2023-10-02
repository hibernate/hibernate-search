/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.provider.impl;

import org.hibernate.search.backend.elasticsearch.types.mapping.impl.Elasticsearch814VectorFieldTypeMappingContributor;
import org.hibernate.search.backend.elasticsearch.types.mapping.impl.ElasticsearchVectorFieldTypeMappingContributor;

import com.google.gson.Gson;

/**
 * The index field type factory provider for ES8.14+.
 */
public class Elasticsearch814IndexFieldTypeFactoryProvider extends AbstractIndexFieldTypeFactoryProvider {

	private final Elasticsearch814VectorFieldTypeMappingContributor vectorFieldTypeMappingContributor =
			new Elasticsearch814VectorFieldTypeMappingContributor();

	public Elasticsearch814IndexFieldTypeFactoryProvider(Gson userFacingGson) {
		super( userFacingGson );
	}

	@Override
	protected ElasticsearchVectorFieldTypeMappingContributor vectorFieldTypeMappingContributor() {
		return vectorFieldTypeMappingContributor;
	}
}
