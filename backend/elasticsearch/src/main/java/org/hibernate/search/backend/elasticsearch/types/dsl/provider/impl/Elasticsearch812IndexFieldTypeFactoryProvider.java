package org.hibernate.search.backend.elasticsearch.types.dsl.provider.impl;

import org.hibernate.search.backend.elasticsearch.json.internal.spi.JsonCodecService;
import org.hibernate.search.backend.elasticsearch.types.mapping.impl.Elasticsearch812VectorFieldTypeMappingContributor;
import org.hibernate.search.backend.elasticsearch.types.mapping.impl.ElasticsearchVectorFieldTypeMappingContributor;

/**
 * The index field type factory provider for ES8.12-8.13.
 */
public class Elasticsearch812IndexFieldTypeFactoryProvider extends AbstractIndexFieldTypeFactoryProvider {

	private final Elasticsearch812VectorFieldTypeMappingContributor vectorFieldTypeMappingContributor =
			new Elasticsearch812VectorFieldTypeMappingContributor();

	public Elasticsearch812IndexFieldTypeFactoryProvider(JsonCodecService userFacingCodec) {
		super( userFacingCodec );
	}

	@Override
	protected ElasticsearchVectorFieldTypeMappingContributor vectorFieldTypeMappingContributor() {
		return vectorFieldTypeMappingContributor;
	}
}
