/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
			int dimension, Integer maxConnections, Integer beamWidth) {
		return new ElasticsearchByteVectorFieldCodec( vectorSimilarity, dimension, maxConnections, beamWidth );
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
