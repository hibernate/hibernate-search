/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.orchestration.impl;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.elasticsearch.work.impl.IndexingWork;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;

/**
 * A thread-safe component ordering and planning the execution of works
 * serially, in the order they were submitted.
 * <p>
 * Serial orchestrators are best used as index-scoped orchestrators,
 * when many works are submitted from different threads:
 * they allow easy implementation of a reasonably safe (though imperfect) concurrency control by expecting
 * the most recent work to hold the most recent data to be indexed.
 * <p>
 * With a serial orchestrator:
 * <ul>
 *     <li>Works are executed in the order they were submitted in.
 *     <li>Two submitted works from the may be sent together in a single bulk request,
 *     but only if all the works between them are bulked too.
 *     <li>The application will wait for already-submitted works to finish when shutting down.
 * </ul>
 * <p>
 * Note that while serial orchestrators preserve ordering as best they can,
 * they lead to a lesser throughput and can only guarantee ordering within a single JVM.
 * When multiple JVMs with multiple instances of Hibernate Search target the same index,
 * the relative order of indexing works might end up being different
 * from the relative order of the database changes that triggered indexing,
 * eventually leading to out-of-sync indexes.
 */
public interface ElasticsearchSerialWorkOrchestrator {

	default <T> CompletableFuture<T> submit(IndexingWork<T> work, OperationSubmitter operationSubmitter) {
		CompletableFuture<T> future = new CompletableFuture<>();
		submit( new ElasticsearchBatchedWork<>( work, future ), operationSubmitter );
		return future;
	}

	default <T> void submit(CompletableFuture<T> future, IndexingWork<T> work, OperationSubmitter operationSubmitter) {
		submit( new ElasticsearchBatchedWork<>( work, future ), operationSubmitter );
	}

	void submit(ElasticsearchBatchedWork<?> work, OperationSubmitter operationSubmitter);

}
