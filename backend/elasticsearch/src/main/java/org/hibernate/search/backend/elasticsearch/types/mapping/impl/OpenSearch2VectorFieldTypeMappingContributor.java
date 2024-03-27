/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.types.mapping.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.DataTypes;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.OpenSearchVectorTypeMethod;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchKnnPredicate;
import org.hibernate.search.backend.elasticsearch.types.impl.ElasticsearchIndexValueFieldType;
import org.hibernate.search.engine.backend.types.VectorSimilarity;
import org.hibernate.search.engine.search.predicate.spi.PredicateTypeKeys;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class OpenSearch2VectorFieldTypeMappingContributor implements ElasticsearchVectorFieldTypeMappingContributor {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final String BYTE_TYPE = "BYTE";

	@Override
	public void contribute(PropertyMapping mapping, Context context) {
		mapping.setType( DataTypes.KNN_VECTOR );
		mapping.setDimension( context.dimension() );
		// float type is default and even if passed it is ignored,
		// we only should pass the byte type:
		if ( BYTE_TYPE.equalsIgnoreCase( context.type() ) ) {
			mapping.setDataType( BYTE_TYPE );
		}

		String resolvedVectorSimilarity = resolveDefault( context.vectorSimilarity() );

		// we want to always set Lucene as an engine:
		OpenSearchVectorTypeMethod method = new OpenSearchVectorTypeMethod();
		method.setName( "hnsw" );
		method.setEngine( "lucene" );
		if ( resolvedVectorSimilarity != null ) {
			method.setSpaceType( resolvedVectorSimilarity );
		}
		if ( context.m() != null || context.efConstruction() != null ) {
			OpenSearchVectorTypeMethod.Parameters parameters = new OpenSearchVectorTypeMethod.Parameters();
			if ( context.m() != null ) {
				parameters.setM( context.m() );
			}
			if ( context.efConstruction() != null ) {
				parameters.setEfConstruction( context.efConstruction() );
			}
			method.setParameters( parameters );
		}

		mapping.setMethod( method );
	}

	@Override
	public <F> void contribute(ElasticsearchIndexValueFieldType.Builder<F> builder, Context context) {
		builder.queryElementFactory( PredicateTypeKeys.KNN,
				new ElasticsearchKnnPredicate.OpenSearchFactory<>( builder.codec() ) );

		builder.contributeAdditionalIndexSettings( settings -> settings.addKnn( true ) );
	}

	private static String resolveDefault(VectorSimilarity vectorSimilarity) {
		switch ( vectorSimilarity ) {
			case DEFAULT:
				return null;
			case L2:
				return "l2";
			case COSINE:
				return "cosinesimil";
			case DOT_PRODUCT:
			case MAX_INNER_PRODUCT:
				throw log.vectorSimilarityNotSupportedByOpenSearchBackend( vectorSimilarity );

			default:
				throw new AssertionFailure( "Unexpected value for Similarity: " + vectorSimilarity );
		}
	}
}
