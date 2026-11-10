package org.hibernate.search.backend.elasticsearch.client.common.spi;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.util.json.spi.JsonResponseConsumerFactory;

/** HTTP transport. Implementations never need to inspect a JSON tree. */
public interface ElasticsearchClient {
	<T> CompletableFuture<ElasticsearchResponse<T>> submit(ElasticsearchRequest request,
			JsonResponseConsumerFactory<T> responseFactory);

	<T> T unwrap(Class<T> clientClass);
}
