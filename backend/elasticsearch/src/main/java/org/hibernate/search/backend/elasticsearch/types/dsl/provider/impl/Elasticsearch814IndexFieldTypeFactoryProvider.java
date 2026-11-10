package org.hibernate.search.backend.elasticsearch.types.dsl.provider.impl;

import org.hibernate.search.backend.elasticsearch.json.internal.spi.JsonCodecService;
import org.hibernate.search.backend.elasticsearch.types.mapping.impl.Elasticsearch814VectorFieldTypeMappingContributor;
import org.hibernate.search.backend.elasticsearch.types.mapping.impl.ElasticsearchVectorFieldTypeMappingContributor;

/**
 * The index field type factory provider for ES8.14+.
 */
public class Elasticsearch814IndexFieldTypeFactoryProvider extends AbstractIndexFieldTypeFactoryProvider {

	private final Elasticsearch814VectorFieldTypeMappingContributor vectorFieldTypeMappingContributor =
			new Elasticsearch814VectorFieldTypeMappingContributor();

	public Elasticsearch814IndexFieldTypeFactoryProvider(JsonCodecService userFacingCodec) {
		super( userFacingCodec );
	}

	@Override
	protected ElasticsearchVectorFieldTypeMappingContributor vectorFieldTypeMappingContributor() {
		return vectorFieldTypeMappingContributor;
	}
}
