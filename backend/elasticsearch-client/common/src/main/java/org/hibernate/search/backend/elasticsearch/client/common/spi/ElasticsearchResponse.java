/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.client.common.spi;

import com.google.gson.JsonObject;

public final class ElasticsearchResponse {
	private final String host;
	private final int statusCode;
	private final String statusMessage;
	private final JsonObject body;

	public ElasticsearchResponse(String host, int statusCode, String statusMessage, JsonObject body) {
		this.host = host;
		this.statusCode = statusCode;
		this.statusMessage = statusMessage;
		this.body = body;
	}

	public String host() {
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
