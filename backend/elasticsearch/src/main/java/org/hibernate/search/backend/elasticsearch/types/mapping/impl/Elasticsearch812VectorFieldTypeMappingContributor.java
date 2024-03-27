/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.types.mapping.impl;

import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.DataTypes;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.ElasticsearchDenseVectorIndexOptions;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchKnnPredicate;
import org.hibernate.search.backend.elasticsearch.types.impl.ElasticsearchIndexValueFieldType;
import org.hibernate.search.engine.backend.types.VectorSimilarity;
import org.hibernate.search.engine.search.predicate.spi.PredicateTypeKeys;
import org.hibernate.search.util.common.AssertionFailure;

public class Elasticsearch812VectorFieldTypeMappingContributor implements ElasticsearchVectorFieldTypeMappingContributor {

	@Override
	public void contribute(PropertyMapping mapping, Context context) {
		mapping.setType( DataTypes.DENSE_VECTOR );
		mapping.setDims( context.dimension() );
		mapping.setElementType( context.type() );
		String resolvedVectorSimilarity = resolveDefault( context.vectorSimilarity() );
		if ( resolvedVectorSimilarity != null ) {
			mapping.setSimilarity( resolvedVectorSimilarity );
		}
		if ( context.m() != null || context.efConstruction() != null ) {
			ElasticsearchDenseVectorIndexOptions indexOptions = new ElasticsearchDenseVectorIndexOptions();
			indexOptions.setType( "hnsw" );
			if ( context.m() != null ) {
				indexOptions.setM( context.m() );
			}
			if ( context.efConstruction() != null ) {
				indexOptions.setEfConstruction( context.efConstruction() );
			}
			mapping.setIndexOptions( indexOptions );
		}
	}

	@Override
	public <F> void contribute(ElasticsearchIndexValueFieldType.Builder<F> builder, Context context) {
		builder.queryElementFactory( PredicateTypeKeys.KNN,
				new ElasticsearchKnnPredicate.Elasticsearch812Factory<>( builder.codec() ) );
	}

	private static String resolveDefault(VectorSimilarity vectorSimilarity) {
		switch ( vectorSimilarity ) {
			case DEFAULT:
				return null;
			case L2:
				return "l2_norm";
			case DOT_PRODUCT:
				return "dot_product";
			case COSINE:
				return "cosine";
			case MAX_INNER_PRODUCT:
				return "max_inner_product";
			default:
				throw new AssertionFailure( "Unexpected value for Similarity: " + vectorSimilarity );
		}
	}
}
