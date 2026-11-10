package org.hibernate.search.backend.elasticsearch.types.dsl;

import com.google.gson.JsonElement;

/** @deprecated Bind an explicit JSON representation with {@code asNative(handler)}. */
@Deprecated
public interface ElasticsearchNativeIndexFieldTypeOptionsStep<S extends ElasticsearchNativeIndexFieldTypeOptionsStep<?>>
		extends ElasticsearchNativeIndexFieldTypeTypedOptionsStep<S, JsonElement> {
}
