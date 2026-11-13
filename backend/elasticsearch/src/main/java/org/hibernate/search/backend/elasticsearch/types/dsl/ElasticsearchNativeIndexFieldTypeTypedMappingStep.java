package org.hibernate.search.backend.elasticsearch.types.dsl;

import org.hibernate.search.util.json.JsonHandler;

/**
 * Defines the Elasticsearch mapping independently of the Java representation of native field values.
 *
 * @param <T> The field value type selected by {@link ElasticsearchIndexFieldTypeFactory#asNative(JsonHandler)}.
 */
public interface ElasticsearchNativeIndexFieldTypeTypedMappingStep<T> {
	/**
	 * Define the Elasticsearch field type and options from JSON text.
	 * This does not change the field value type {@code T} or its value handler.
	 *
	 * @param json A JSON object defining the Elasticsearch mapping, for example {@code {"type":"ip"}}.
	 * @return A DSL step where the native field options can be defined.
	 */
	ElasticsearchNativeIndexFieldTypeTypedOptionsStep<?, T> mapping(String json);

	/**
	 * Define the Elasticsearch field type and options from a JSON object represented by {@code M}.
	 * The given handler only reads this mapping definition. It can use a different representation
	 * from the field value handler selected by {@link ElasticsearchIndexFieldTypeFactory#asNative(JsonHandler)}.
	 * The field value type {@code T} and the index-side type expected from a bridge remain unchanged.
	 *
	 * @param handler The handler for the mapping definition, independently of the field value handler.
	 * @param json A JSON object defining the Elasticsearch mapping.
	 * @param <M> The Java representation of the mapping definition.
	 * @return A DSL step where the native field options can be defined, still using field value type {@code T}.
	 */
	<M> ElasticsearchNativeIndexFieldTypeTypedOptionsStep<?, T> mapping(JsonHandler<M> handler, M json);
}
