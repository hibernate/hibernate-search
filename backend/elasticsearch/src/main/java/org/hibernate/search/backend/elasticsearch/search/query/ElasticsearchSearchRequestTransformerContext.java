package org.hibernate.search.backend.elasticsearch.search.query;

import java.util.Map;
import java.util.function.UnaryOperator;

import org.hibernate.search.util.common.annotation.Incubating;
import org.hibernate.search.util.json.JsonHandler;

import com.google.gson.JsonObject;

/**
 * The context passed to {@link ElasticsearchSearchRequestTransformer#transform(ElasticsearchSearchRequestTransformerContext)}.
 * <p>
 * <strong>WARNING:</strong> Direct changes to the request may conflict with Hibernate Search features
 * and be supported differently by different versions of Elasticsearch.
 * Thus they cannot be guaranteed to continue to work when upgrading Hibernate Search,
 * even for micro upgrades ({@code x.y.z} to {@code x.y.(z+1)}).
 * Use this at your own risk.
 */
@Incubating
public interface ElasticsearchSearchRequestTransformerContext {

	/**
	 * @return The URL-encoded path of the HTTP request for this search.
	 */
	String path();

	/**
	 * Set a new path for the HTTP request for this search.
	 *
	 * @param newPath A HTTP path, already URL-encoded.
	 * Should not include the query parameters ({@code ?foo=bar&...}):
	 * use {@link #parametersMap()} for that instead.
	 */
	void path(String newPath);

	/**
	 * @return A (mutable) representation of the HTTP query parameters for this search, as a {@link Map}.
	 * The query parameters are <strong>not</strong> URL-encoded.
	 */
	Map<String, String> parametersMap();

	/**
	 * @return A (mutable) representation of the HTTP request body for this search, as a {@link JsonObject}.
	 */
	@Deprecated
	JsonObject body();

	/**
	 * Transform an independent representation of the current staged JSON object body.
	 * The returned tree is installed after success, with no second defensive copy.
	 * Do not mutate it afterward. Mixing this with {@link #body()} is rejected.
	 * @param handler The JSON-library representation to use.
	 * @param transform A callback returning a non-null JSON object.
	 * @param <T> The library's JSON tree/value type.
	 */
	<T> void transformBody(JsonHandler<T> handler, UnaryOperator<T> transform);

}
