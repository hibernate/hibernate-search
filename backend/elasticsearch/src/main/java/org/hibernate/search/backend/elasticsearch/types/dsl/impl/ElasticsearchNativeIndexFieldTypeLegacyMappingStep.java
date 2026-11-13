package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import org.hibernate.search.backend.elasticsearch.json.impl.JsonHandlerBridge;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.types.dsl.ElasticsearchNativeIndexFieldTypeMappingStep;
import org.hibernate.search.backend.elasticsearch.types.dsl.ElasticsearchNativeIndexFieldTypeOptionsStep;
import org.hibernate.search.util.json.JsonHandler;

@SuppressWarnings("deprecation")
class ElasticsearchNativeIndexFieldTypeLegacyMappingStep implements ElasticsearchNativeIndexFieldTypeMappingStep {
	private final ElasticsearchIndexFieldTypeBuildContext buildContext;

	ElasticsearchNativeIndexFieldTypeLegacyMappingStep(ElasticsearchIndexFieldTypeBuildContext buildContext) {
		this.buildContext = buildContext;
	}

	@Override
	public ElasticsearchNativeIndexFieldTypeOptionsStep<?> mapping(String json) {
		PropertyMapping mapping = buildContext.getUserFacingCodec().decode(
				JsonHandlerBridge.parseObject( json, "native field mapping", buildContext.getUserFacingCodec().parsing() ),
				PropertyMapping.class );
		return new ElasticsearchNativeIndexFieldTypeLegacyOptionsStep( buildContext, mapping );
	}

	@Override
	public <M> ElasticsearchNativeIndexFieldTypeOptionsStep<?> mapping(JsonHandler<M> handler, M json) {
		PropertyMapping mapping = buildContext.getUserFacingCodec().decode(
				JsonHandlerBridge.importObject( handler, json, "native field mapping",
						buildContext.getUserFacingCodec().parsing() ),
				PropertyMapping.class );
		return new ElasticsearchNativeIndexFieldTypeLegacyOptionsStep( buildContext, mapping );
	}
}
