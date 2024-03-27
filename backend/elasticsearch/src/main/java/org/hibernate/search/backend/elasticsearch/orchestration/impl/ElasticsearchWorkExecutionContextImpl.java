/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.orchestration.impl;

import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchClient;
import org.hibernate.search.backend.elasticsearch.gson.spi.GsonProvider;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchWorkExecutionContext;

/**
 * The execution context for works.
 * <p>
 * This context is immutable and thread-safe.
 */
class ElasticsearchWorkExecutionContextImpl implements ElasticsearchWorkExecutionContext {

	private final ElasticsearchClient client;
	private final GsonProvider gsonProvider;

	public ElasticsearchWorkExecutionContextImpl(ElasticsearchClient client, GsonProvider gsonProvider) {
		this.client = client;
		this.gsonProvider = gsonProvider;
	}

	@Override
	public ElasticsearchClient getClient() {
		return client;
	}

	@Override
	public GsonProvider getGsonProvider() {
		return gsonProvider;
	}

}
