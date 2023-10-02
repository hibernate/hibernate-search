/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.logging.impl;

import org.hibernate.search.backend.elasticsearch.gson.spi.JsonLogHelper;

import com.google.gson.JsonObject;

/**
 * Used with JBoss Logging's {@link org.jboss.logging.annotations.FormatWith}
 * to display {@link JsonObject}s in log messages.
 *
 */
public final class ElasticsearchJsonObjectFormatter {

	private final JsonObject object;

	public ElasticsearchJsonObjectFormatter(JsonObject object) {
		this.object = object;
	}

	@Override
	public String toString() {
		return JsonLogHelper.get().toString( object );
	}
}
