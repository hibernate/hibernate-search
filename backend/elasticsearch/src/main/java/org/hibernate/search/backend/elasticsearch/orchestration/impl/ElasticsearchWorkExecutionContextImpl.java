package org.hibernate.search.backend.elasticsearch.orchestration.impl;

import org.hibernate.search.backend.elasticsearch.client.common.impl.ElasticsearchJsonClient;
import org.hibernate.search.backend.elasticsearch.client.common.json.internal.spi.JsonProvider;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchWorkExecutionContext;

/**
 * The execution context for works.
 * <p>
 * This context is immutable and thread-safe.
 */
class ElasticsearchWorkExecutionContextImpl implements ElasticsearchWorkExecutionContext {

	private final ElasticsearchJsonClient client;
	private final JsonProvider jsonProvider;

	public ElasticsearchWorkExecutionContextImpl(ElasticsearchJsonClient client, JsonProvider jsonProvider) {
		this.client = client;
		this.jsonProvider = jsonProvider;
	}

	@Override
	public ElasticsearchJsonClient getClient() {
		return client;
	}

	@Override
	public JsonProvider getJsonProvider() {
		return jsonProvider;
	}

}
