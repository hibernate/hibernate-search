/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import org.hibernate.search.backend.elasticsearch.types.codec.impl.AbstractElasticsearchVectorFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchByteVectorFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.mapping.impl.ElasticsearchVectorFieldTypeMappingContributor;
import org.hibernate.search.engine.backend.types.VectorSimilarity;

class ElasticsearchByteVectorFieldTypeOptionsStep
		extends
		AbstractElasticsearchVectorFieldTypeOptionsStep<ElasticsearchByteVectorFieldTypeOptionsStep, byte[]> {

	ElasticsearchByteVectorFieldTypeOptionsStep(ElasticsearchIndexFieldTypeBuildContext buildContext,
			ElasticsearchVectorFieldTypeMappingContributor mappingContributor) {
		super( buildContext, byte[].class, mappingContributor );
	}

	@Override
	protected AbstractElasticsearchVectorFieldCodec<byte[]> createCodec(VectorSimilarity vectorSimilarity,
			int dimension, Integer m, Integer efConstruction, byte[] indexNullAs) {
		return new ElasticsearchByteVectorFieldCodec( vectorSimilarity, dimension, m, efConstruction, indexNullAs );
	}

	@Override
	public String type() {
		return "byte";
	}

	@Override
	protected ElasticsearchByteVectorFieldTypeOptionsStep thisAsS() {
		return this;
	}
}
