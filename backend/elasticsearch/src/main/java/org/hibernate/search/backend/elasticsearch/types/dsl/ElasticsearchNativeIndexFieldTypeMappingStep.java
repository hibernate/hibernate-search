package org.hibernate.search.backend.elasticsearch.types.dsl;

import org.hibernate.search.backend.elasticsearch.gson.GsonJsonHandlers;
import org.hibernate.search.util.json.JsonHandler;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/** @deprecated Bind an explicit JSON representation with {@code asNative(handler)}. */
@Deprecated
public interface ElasticsearchNativeIndexFieldTypeMappingStep
		extends ElasticsearchNativeIndexFieldTypeTypedMappingStep<JsonElement> {
	default ElasticsearchNativeIndexFieldTypeOptionsStep<?> mapping(JsonObject json) {
		return mapping( GsonJsonHandlers.object(), json );
	}

	@Override
	ElasticsearchNativeIndexFieldTypeOptionsStep<?> mapping(String json);

	@Override
	<M> ElasticsearchNativeIndexFieldTypeOptionsStep<?> mapping(JsonHandler<M> handler, M json);
}
