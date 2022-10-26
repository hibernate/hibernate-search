/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.orchestration.impl;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.elasticsearch.work.impl.NonBulkableWork;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;

/**
 * A thread-safe component planning the execution of works
 * in parallel, without any consideration for the order they were submitted in.
 * <p>
 * Parallel orchestrators are suitable when the client takes the responsibility
 * of submitting works as needed to implement ordering: if work #2 must be executed after work #1,
 * the client will take care of waiting until #1 is done before he submits #2.
 * <p>
 * With a parallel orchestrator:
 * <ul>
 *     <li>Works are executed in unpredictable order, irrespective of the order they were submitted in.
 *     <li>Two submitted works may be sent together in a single bulk request.
 *     <li>The application will <strong>not</strong> wait for already-submitted works to finish when shutting down.
 * </ul>
 */
public interface ElasticsearchParallelWorkOrchestrator {

	<T> CompletableFuture<T> submit(NonBulkableWork<T> work, OperationSubmitter operationSubmitter);

}
