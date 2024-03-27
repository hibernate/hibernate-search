/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.provider.impl;

import org.hibernate.search.backend.elasticsearch.types.mapping.impl.Elasticsearch812VectorFieldTypeMappingContributor;
import org.hibernate.search.backend.elasticsearch.types.mapping.impl.ElasticsearchVectorFieldTypeMappingContributor;

import com.google.gson.Gson;

/**
 * The index field type factory provider for ES8.12+.
 */
public class Elasticsearch812IndexFieldTypeFactoryProvider extends AbstractIndexFieldTypeFactoryProvider {

	private final Elasticsearch812VectorFieldTypeMappingContributor vectorFieldTypeMappingContributor =
			new Elasticsearch812VectorFieldTypeMappingContributor();

	public Elasticsearch812IndexFieldTypeFactoryProvider(Gson userFacingGson) {
		super( userFacingGson );
	}

	@Override
	protected ElasticsearchVectorFieldTypeMappingContributor vectorFieldTypeMappingContributor() {
		return vectorFieldTypeMappingContributor;
	}
}
