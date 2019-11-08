/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.query;

import java.util.Map;

import com.google.gson.JsonObject;

/**
 * The context passed to {@link ElasticsearchSearchRequestTransformer#transform(ElasticsearchSearchRequestTransformerContext)}.
 * <p>
 * <strong>WARNING:</strong> Direct changes to the request may conflict with Hibernate Search features
 * and be supported differently by different versions of Elasticsearch.
 * Thus they cannot be guaranteed to continue to work when upgrading Hibernate Search,
 * even for micro upgrades ({@code x.y.z} to {@code x.y.(z+1)}).
 * Use this at your own risk.
 *
 * @hsearch.experimental This type is under active development.
 *    Usual compatibility policies do not apply: incompatible changes may be introduced in any future release.
 */
public interface ElasticsearchSearchRequestTransformerContext {

	/**
	 * @return The URL-encoded path of the HTTP request for this search.
	 */
	String getPath();

	/**
	 * Set a new path for the HTTP request for this search.
	 *
	 * @param newPath A HTTP path, already URL-encoded.
	 * Should not include the query parameters ({@code ?foo=bar&...}):
	 * use {@link #getParametersMap()} for that instead.
	 */
	void setPath(String newPath);

	/**
	 * @return A (mutable) representation of the HTTP query parameters for this search, as a {@link Map}.
	 * The query parameters are <strong>not</strong> URL-encoded.
	 */
	Map<String, String> getParametersMap();

	/**
	 * @return A (mutable) representation of the HTTP request body for this search, as a {@link JsonObject}.
	 */
	JsonObject getBody();

}
