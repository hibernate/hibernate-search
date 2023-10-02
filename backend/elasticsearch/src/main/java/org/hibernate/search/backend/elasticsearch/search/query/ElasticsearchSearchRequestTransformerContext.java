/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.search.query;

import java.util.Map;

import org.hibernate.search.util.common.annotation.Incubating;

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
	JsonObject body();

}
