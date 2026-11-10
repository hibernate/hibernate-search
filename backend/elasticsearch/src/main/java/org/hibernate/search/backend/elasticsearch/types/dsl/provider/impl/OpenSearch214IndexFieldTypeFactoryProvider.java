package org.hibernate.search.backend.elasticsearch.types.dsl.provider.impl;

import org.hibernate.search.backend.elasticsearch.json.internal.spi.JsonCodecService;
import org.hibernate.search.backend.elasticsearch.types.mapping.impl.ElasticsearchVectorFieldTypeMappingContributor;
import org.hibernate.search.backend.elasticsearch.types.mapping.impl.OpenSearch214VectorFieldTypeMappingContributor;

/**
 * The index field type factory provider for OpenSearch 2.14+.
 */
public class OpenSearch214IndexFieldTypeFactoryProvider extends AbstractIndexFieldTypeFactoryProvider {

	private final OpenSearch214VectorFieldTypeMappingContributor vectorFieldTypeMappingContributor =
			new OpenSearch214VectorFieldTypeMappingContributor();

	public OpenSearch214IndexFieldTypeFactoryProvider(JsonCodecService userFacingCodec) {
		super( userFacingCodec );
	}

	@Override
	protected ElasticsearchVectorFieldTypeMappingContributor vectorFieldTypeMappingContributor() {
		return vectorFieldTypeMappingContributor;
	}
}
