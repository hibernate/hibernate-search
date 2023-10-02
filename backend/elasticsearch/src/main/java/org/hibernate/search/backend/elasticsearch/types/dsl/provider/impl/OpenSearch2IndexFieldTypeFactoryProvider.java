/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.provider.impl;

import org.hibernate.search.backend.elasticsearch.types.mapping.impl.ElasticsearchVectorFieldTypeMappingContributor;
import org.hibernate.search.backend.elasticsearch.types.mapping.impl.OpenSearch2VectorFieldTypeMappingContributor;

import com.google.gson.Gson;

/**
 * The index field type factory provider for OpenSearch 2.x.
 */
public class OpenSearch2IndexFieldTypeFactoryProvider extends AbstractIndexFieldTypeFactoryProvider {

	private final OpenSearch2VectorFieldTypeMappingContributor vectorFieldTypeMappingContributor =
			new OpenSearch2VectorFieldTypeMappingContributor();

	public OpenSearch2IndexFieldTypeFactoryProvider(Gson userFacingGson) {
		super( userFacingGson );
	}

	@Override
	protected ElasticsearchVectorFieldTypeMappingContributor vectorFieldTypeMappingContributor() {
		return vectorFieldTypeMappingContributor;
	}
}
