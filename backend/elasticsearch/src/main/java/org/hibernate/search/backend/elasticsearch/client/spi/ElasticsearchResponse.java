/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.client.spi;

import com.google.gson.JsonObject;

import org.apache.http.HttpHost;

public final class ElasticsearchResponse {

	private final HttpHost host;
	private final int statusCode;

	private final String statusMessage;

	private final JsonObject body;

	public ElasticsearchResponse(HttpHost host, int statusCode, String statusMessage, JsonObject body) {
		this.host = host;
		this.statusCode = statusCode;
		this.statusMessage = statusMessage;
		this.body = body;
	}

	public HttpHost host() {
		return host;
	}

	public int statusCode() {
		return statusCode;
	}

	public String statusMessage() {
		return statusMessage;
	}

	public JsonObject body() {
		return body;
	}

}
