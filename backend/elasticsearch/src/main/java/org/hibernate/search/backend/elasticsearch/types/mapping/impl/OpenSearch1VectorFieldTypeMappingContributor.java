/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.mapping.impl;

import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.DataTypes;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.PropertyMapping;
import org.hibernate.search.engine.backend.types.VectorSimilarity;
import org.hibernate.search.util.common.AssertionFailure;

import com.google.gson.JsonObject;

public class OpenSearch1VectorFieldTypeMappingContributor implements ElasticsearchVectorFieldTypeMappingContributor {

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
		JsonObject method = new JsonObject();
		method.addProperty( "name", "hnsw" );
		method.addProperty( "engine", "lucene" );
		if ( resolvedVectorSimilarity != null ) {
			method.addProperty( "space_type", resolvedVectorSimilarity );
		}
		if ( context.maxConnections() != null || context.beamWidth() != null ) {
			JsonObject parameters = new JsonObject();
			if ( context.maxConnections() != null ) {
				parameters.addProperty( "m", context.maxConnections() );
			}
			if ( context.beamWidth() != null ) {
				parameters.addProperty( "ef_construction", context.beamWidth() );
			}
			method.add( "parameters", parameters );
		}

		mapping.setMethod( method );
	}

	private static String resolveDefault(VectorSimilarity vectorSimilarity) {
		switch ( vectorSimilarity ) {
			case DEFAULT:
				return null;
			case L2:
				return "l2";
			case INNER_PRODUCT:
				// TODO: figure out what's actually supported: innerproduct/l1/linf all throw exceptions ...
				//   see HSEARCH-5038
				return "l2";
			case COSINE:
				return "cosinesimil";
			default:
				throw new AssertionFailure( "Unexpected value for Similarity: " + vectorSimilarity );
		}
	}
}