package org.hibernate.search.backend.elasticsearch.work.impl;

import java.util.concurrent.CompletableFuture;

public interface NonBulkableWork<T> extends ElasticsearchWork {

	CompletableFuture<T> execute(ElasticsearchWorkExecutionContext context);

}
