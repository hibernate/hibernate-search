/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.orchestration.impl;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.elasticsearch.work.impl.IndexingWork;
import org.hibernate.search.engine.backend.orchestration.spi.BatchedWork;
import org.hibernate.search.util.common.impl.Futures;

class ElasticsearchBatchedWork<T> implements BatchedWork<ElasticsearchBatchedWorkProcessor> {
	private final IndexingWork<T> work;
	private final CompletableFuture<T> future;

	ElasticsearchBatchedWork(IndexingWork<T> work, CompletableFuture<T> future) {
		this.work = work;
		this.future = future;
	}

	@Override
	public void submitTo(ElasticsearchBatchedWorkProcessor delegate) {
		delegate.submit( work ).whenComplete( Futures.copyHandler( future ) );
	}

	@Override
	public void markAsFailed(Throwable t) {
		future.completeExceptionally( t );
	}

	String getQueuingKey() {
		return work.getQueuingKey();
	}
}
