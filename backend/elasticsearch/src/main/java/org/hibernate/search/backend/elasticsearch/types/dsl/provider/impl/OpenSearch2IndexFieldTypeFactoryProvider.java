package org.hibernate.search.backend.elasticsearch.types.dsl.provider.impl;

import org.hibernate.search.backend.elasticsearch.json.internal.spi.JsonCodecService;
import org.hibernate.search.backend.elasticsearch.types.mapping.impl.ElasticsearchVectorFieldTypeMappingContributor;
import org.hibernate.search.backend.elasticsearch.types.mapping.impl.OpenSearch2VectorFieldTypeMappingContributor;

/**
 * The index field type factory provider for OpenSearch 2.x.
 */
public class OpenSearch2IndexFieldTypeFactoryProvider extends AbstractIndexFieldTypeFactoryProvider {

	private final OpenSearch2VectorFieldTypeMappingContributor vectorFieldTypeMappingContributor =
			new OpenSearch2VectorFieldTypeMappingContributor();

	public OpenSearch2IndexFieldTypeFactoryProvider(JsonCodecService userFacingCodec) {
		super( userFacingCodec );
	}

	@Override
	protected ElasticsearchVectorFieldTypeMappingContributor vectorFieldTypeMappingContributor() {
		return vectorFieldTypeMappingContributor;
	}
}
