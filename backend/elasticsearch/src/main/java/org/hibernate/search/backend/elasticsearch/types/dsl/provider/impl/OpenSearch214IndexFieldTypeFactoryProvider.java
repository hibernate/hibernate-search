/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.provider.impl;

import org.hibernate.search.backend.elasticsearch.types.mapping.impl.ElasticsearchVectorFieldTypeMappingContributor;
import org.hibernate.search.backend.elasticsearch.types.mapping.impl.OpenSearch214VectorFieldTypeMappingContributor;

import com.google.gson.Gson;

/**
 * The index field type factory provider for OpenSearch 2.14+.
 */
public class OpenSearch214IndexFieldTypeFactoryProvider extends AbstractIndexFieldTypeFactoryProvider {

	private final OpenSearch214VectorFieldTypeMappingContributor vectorFieldTypeMappingContributor =
			new OpenSearch214VectorFieldTypeMappingContributor();

	public OpenSearch214IndexFieldTypeFactoryProvider(Gson userFacingGson) {
		super( userFacingGson );
	}

	@Override
	protected ElasticsearchVectorFieldTypeMappingContributor vectorFieldTypeMappingContributor() {
		return vectorFieldTypeMappingContributor;
	}
}
