/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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

public class Elasticsearch8VectorFieldTypeMappingContributor implements ElasticsearchVectorFieldTypeMappingContributor {
	@Override
	public void contribute(PropertyMapping mapping, Context context) {
		mapping.setType( DataTypes.DENSE_VECTOR );
		mapping.setDims( context.dimension() );
		mapping.setElementType( context.type() );
		String resolvedVectorSimilarity = resolveDefault( context.vectorSimilarity() );
		if ( resolvedVectorSimilarity != null ) {
			mapping.setSimilarity( resolvedVectorSimilarity );
		}
		if ( context.maxConnections() != null || context.beamWidth() != null ) {
			ElasticsearchDenseVectorIndexOptions indexOptions = new ElasticsearchDenseVectorIndexOptions();
			indexOptions.setType( "hnsw" );
			if ( context.maxConnections() != null ) {
				indexOptions.setM( context.maxConnections() );
			}
			if ( context.beamWidth() != null ) {
				indexOptions.setEfConstruction( context.beamWidth() );
			}
			mapping.setIndexOptions( indexOptions );
		}
	}

	@Override
	public <F> void contribute(ElasticsearchIndexValueFieldType.Builder<F> builder, Context context) {
		builder.queryElementFactory( PredicateTypeKeys.KNN,
				new ElasticsearchKnnPredicate.ElasticsearchFactory<>( builder.codec() ) );
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
