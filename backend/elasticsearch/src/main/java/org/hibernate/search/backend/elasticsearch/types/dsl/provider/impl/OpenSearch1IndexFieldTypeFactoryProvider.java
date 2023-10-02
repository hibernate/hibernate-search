/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.provider.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.types.impl.ElasticsearchIndexValueFieldType;
import org.hibernate.search.backend.elasticsearch.types.mapping.impl.ElasticsearchVectorFieldTypeMappingContributor;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import com.google.gson.Gson;

/**
 * The index field type factory provider for OpenSearch 1.x.
 */
public class OpenSearch1IndexFieldTypeFactoryProvider extends AbstractIndexFieldTypeFactoryProvider {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final ElasticsearchVectorFieldTypeMappingContributor vectorFieldTypeMappingContributor =
			new ElasticsearchVectorFieldTypeMappingContributor() {

				@Override
				public void contribute(PropertyMapping mapping, Context context) {
					throw log.searchBackendVersionIncompatibleWithVectorIntegration( "OpenSearch", "2.x" );
				}

				@Override
				public <F> void contribute(ElasticsearchIndexValueFieldType.Builder<F> builder, Context context) {
					throw log.searchBackendVersionIncompatibleWithVectorIntegration( "OpenSearch", "2.x" );
				}
			};

	public OpenSearch1IndexFieldTypeFactoryProvider(Gson userFacingGson) {
		super( userFacingGson );
	}

	@Override
	protected ElasticsearchVectorFieldTypeMappingContributor vectorFieldTypeMappingContributor() {
		return vectorFieldTypeMappingContributor;
	}
}
