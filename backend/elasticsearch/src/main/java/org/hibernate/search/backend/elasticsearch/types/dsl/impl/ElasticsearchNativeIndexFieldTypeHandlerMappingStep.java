package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import java.util.Objects;

import org.hibernate.search.backend.elasticsearch.json.impl.JsonHandlerBridge;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.types.dsl.ElasticsearchNativeIndexFieldTypeTypedMappingStep;
import org.hibernate.search.backend.elasticsearch.types.dsl.ElasticsearchNativeIndexFieldTypeTypedOptionsStep;
import org.hibernate.search.util.json.JsonHandler;

class ElasticsearchNativeIndexFieldTypeHandlerMappingStep<T> implements ElasticsearchNativeIndexFieldTypeTypedMappingStep<T> {
	private final ElasticsearchIndexFieldTypeBuildContext buildContext;
	private final JsonHandler<T> handler;

	ElasticsearchNativeIndexFieldTypeHandlerMappingStep(ElasticsearchIndexFieldTypeBuildContext buildContext,
			JsonHandler<T> handler) {
		this.buildContext = buildContext;
		this.handler = Objects.requireNonNull( handler );
		Objects.requireNonNull( handler.javaType() );
	}

	@Override
	public ElasticsearchNativeIndexFieldTypeTypedOptionsStep<?, T> mapping(String json) {
		PropertyMapping mapping = buildContext.getUserFacingCodec().decode(
				JsonHandlerBridge.parseObject( json, "native field mapping", buildContext.getUserFacingCodec().parsing() ),
				PropertyMapping.class );
		return new ElasticsearchNativeIndexFieldTypeHandlerOptionsStep<>( buildContext, mapping, handler );
	}

	@Override
	public <M> ElasticsearchNativeIndexFieldTypeTypedOptionsStep<?, T> mapping(JsonHandler<M> mappingHandler, M json) {
		PropertyMapping mapping = buildContext.getUserFacingCodec().decode(
				JsonHandlerBridge.importObject( mappingHandler, json, "native field mapping",
						buildContext.getUserFacingCodec().parsing() ),
				PropertyMapping.class );
		return new ElasticsearchNativeIndexFieldTypeHandlerOptionsStep<>( buildContext, mapping, handler );
	}
}
