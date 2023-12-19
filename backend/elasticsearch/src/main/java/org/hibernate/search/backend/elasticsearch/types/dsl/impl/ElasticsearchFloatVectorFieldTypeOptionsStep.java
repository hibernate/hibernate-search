/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import org.hibernate.search.backend.elasticsearch.types.codec.impl.AbstractElasticsearchVectorFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFloatVectorFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.mapping.impl.ElasticsearchVectorFieldTypeMappingContributor;
import org.hibernate.search.engine.backend.types.VectorSimilarity;

class ElasticsearchFloatVectorFieldTypeOptionsStep
		extends
		AbstractElasticsearchVectorFieldTypeOptionsStep<ElasticsearchFloatVectorFieldTypeOptionsStep, float[]> {

	ElasticsearchFloatVectorFieldTypeOptionsStep(ElasticsearchIndexFieldTypeBuildContext buildContext,
			ElasticsearchVectorFieldTypeMappingContributor mappingContributor) {
		super( buildContext, float[].class, mappingContributor );
	}


	@Override
	public String type() {
		return "float";
	}

	@Override
	protected AbstractElasticsearchVectorFieldCodec<float[]> createCodec(VectorSimilarity similarity, int dimension,
			Integer maxConnections, Integer beamWidth, float[] indexNullAs) {
		return new ElasticsearchFloatVectorFieldCodec( similarity, dimension, maxConnections, beamWidth, indexNullAs );
	}

	@Override
	protected ElasticsearchFloatVectorFieldTypeOptionsStep thisAsS() {
		return this;
	}
}
