/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.orchestration.impl;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchWork;
import org.hibernate.search.engine.backend.orchestration.spi.BatchedWork;

/**
 * A thread-safe component responsible for ordering and planning the execution of works.
 */
public interface ElasticsearchWorkOrchestrator {

	default <T> CompletableFuture<T> submit(ElasticsearchWork<T> work) {
		CompletableFuture<T> future = new CompletableFuture<>();
		submit( new ElasticsearchBatchedWork<>( work, future ) );
		return future;
	}

	default <T> void submit(CompletableFuture<T> future, ElasticsearchWork<T> work) {
		submit( new ElasticsearchBatchedWork<>( work, future ) );
	}

	void submit(BatchedWork<ElasticsearchWorkProcessor> work);

}
