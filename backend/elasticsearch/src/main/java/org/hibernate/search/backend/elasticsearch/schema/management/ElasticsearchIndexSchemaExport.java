package org.hibernate.search.backend.elasticsearch.schema.management;

import java.util.List;
import java.util.Map;

import org.hibernate.search.engine.common.schema.management.SchemaExport;
import org.hibernate.search.util.common.annotation.Incubating;
import org.hibernate.search.util.json.JsonHandler;

import com.google.gson.JsonObject;

/** Extended version of an {@link SchemaExport} exposing Elasticsearch request parts. */
@Incubating
public interface ElasticsearchIndexSchemaExport extends SchemaExport {

	/** @return Query parameters required to create the exported index. */
	Map<String, String> parameters();

	/**
	 * @return The body objects required to create the exported index, converted to Gson.
	 * @deprecated Use {@link #bodyParts(JsonHandler)} with the handler for your JSON library.
	 */
	@Deprecated
	List<JsonObject> bodyParts();

	/**
	 * Export request body objects through an explicit JSON-library handler.
	 * Results are borrowed, read-only values; callers must not mutate them.
	 * @param handler The handler for the application's JSON object type.
	 * @param <T> The JSON-library value type.
	 * @return An unmodifiable list of body objects.
	 */
	<T> List<T> bodyParts(JsonHandler<T> handler);
}
