/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.orchestration.impl;


import java.util.concurrent.CompletableFuture;

public interface LuceneWriteWorkOrchestratorImplementor extends LuceneWriteWorkOrchestrator {

	/**
	 * Start any resource necessary to operate the orchestrator at runtime.
	 * <p>
	 * Must be called to switch the orchestrator to the "running" state,
	 * in which works can be submitted.
	 */
	void start();

	/**
	 * Executes a special asynchronous operation checking that the index actually exists or creating it if necessary.
	 * <p>
	 * If the index does not exist, a temporary index writer will be created to create the index.
	 * If the index already exists, no index writer is created.
	 */
	CompletableFuture<?> ensureIndexExists();

	/**
	 * Stop accepting works and return a future that completes when all works have been completely executed.
	 * <p>
	 * Optionally called by the owner of this orchestrator before {@link #stop()},
	 * if it needs to wait for work completion.
	 *
	 * @return A future that completes when all ongoing works have been completely executed.
	 */
	CompletableFuture<?> preStop();

	/**
	 * Forcibly stop ongoing work (if possible) and release any resource necessary to operate the orchestrator at runtime.
	 * <p>
	 * Must be called by the owner of this orchestrator before shutdown.
	 */
	void stop();

}
