package org.hibernate.search.backend.elasticsearch.types.dsl;

import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeOptionsStep;

import com.google.gson.JsonElement;

public interface ElasticsearchNativeIndexFieldTypeOptionsStep<S extends ElasticsearchNativeIndexFieldTypeOptionsStep<?>>
		extends IndexFieldTypeOptionsStep<S, JsonElement> {

}
