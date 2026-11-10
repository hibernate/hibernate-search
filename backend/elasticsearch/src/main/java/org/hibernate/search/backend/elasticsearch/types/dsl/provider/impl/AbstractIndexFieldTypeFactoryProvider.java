package org.hibernate.search.backend.elasticsearch.types.dsl.provider.impl;

import org.hibernate.search.backend.elasticsearch.json.internal.spi.JsonCodecService;
import org.hibernate.search.backend.elasticsearch.types.dsl.ElasticsearchIndexFieldTypeFactory;
import org.hibernate.search.backend.elasticsearch.types.dsl.impl.ElasticsearchIndexFieldTypeFactoryImpl;
import org.hibernate.search.backend.elasticsearch.types.format.impl.Elasticsearch7DefaultFieldFormatProvider;
import org.hibernate.search.backend.elasticsearch.types.mapping.impl.ElasticsearchVectorFieldTypeMappingContributor;
import org.hibernate.search.engine.backend.mapping.spi.BackendMapperContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexFieldTypeDefaultsProvider;
import org.hibernate.search.util.common.reporting.EventContext;

abstract class AbstractIndexFieldTypeFactoryProvider
		implements ElasticsearchIndexFieldTypeFactoryProvider {

	private final JsonCodecService userFacingCodec;
	private final Elasticsearch7DefaultFieldFormatProvider defaultFieldFormatProvider =
			new Elasticsearch7DefaultFieldFormatProvider();

	public AbstractIndexFieldTypeFactoryProvider(JsonCodecService userFacingCodec) {
		this.userFacingCodec = userFacingCodec;
	}

	@Override
	public final ElasticsearchIndexFieldTypeFactory create(EventContext eventContext,
			BackendMapperContext backendMapperContext, IndexFieldTypeDefaultsProvider typeDefaultsProvider) {
		return new ElasticsearchIndexFieldTypeFactoryImpl(
				eventContext, backendMapperContext, userFacingCodec, defaultFieldFormatProvider, typeDefaultsProvider,
				vectorFieldTypeMappingContributor()
		);
	}

	protected abstract ElasticsearchVectorFieldTypeMappingContributor vectorFieldTypeMappingContributor();
}
