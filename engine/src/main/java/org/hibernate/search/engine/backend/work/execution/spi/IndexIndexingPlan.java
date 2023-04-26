/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.work.execution.spi;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.backend.common.spi.MultiEntityOperationExecutionReport;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.util.common.impl.Throwables;

/**
 * A set of works to be executed on a single index.
 * <p>
 * Works are accumulated when methods such as {@link #add(DocumentReferenceProvider, DocumentContributor)}
 * or {@link #addOrUpdate(DocumentReferenceProvider, DocumentContributor)} are called,
 * and executed only when {@link #execute(OperationSubmitter)} is called.
 * <p>
 * Relative ordering of works within a plan will be preserved.
 * <p>
 * Implementations may not be thread-safe.
 */
public interface IndexIndexingPlan {

	/**
	 * Add a document to the index, assuming that the document is absent from the index.
	 *
	 * @param documentReferenceProvider A source of information about the identity of the document to add.
	 * @param documentContributor A contributor to the document, adding fields to the indexed document.
	 */
	void add(DocumentReferenceProvider documentReferenceProvider, DocumentContributor documentContributor);

	/**
	 * Update a document in the index, or add it if it's absent from the index.
	 *
	 * @param documentReferenceProvider A source of information about the identity of the document to update.
	 * @param documentContributor A contributor to the document, adding fields to the indexed document.
	 */
	void addOrUpdate(DocumentReferenceProvider documentReferenceProvider, DocumentContributor documentContributor);

	/**
	 * Delete a document from the index.
	 *
	 * @param documentReferenceProvider A source of information about the identity of the document to delete.
	 */
	void delete(DocumentReferenceProvider documentReferenceProvider);

	/**
	 * Start executing all the works in this plan, and clear the plan so that it can be re-used.
	 *
	 * @return A {@link CompletableFuture} that will be completed when all the works are complete.
	 * The future will be completed with an exception if a work failed.
	 */
	default CompletableFuture<?> execute(OperationSubmitter operationSubmitter) {
		return executeAndReport( operationSubmitter ).thenApply( report -> {
				report.throwable().ifPresent( t -> {
					throw Throwables.toRuntimeException( t );
				} );
				return null;
			} );
	}

	/**
	 * Start executing all the works in this plan, and clear the plan so that it can be re-used.
	 *
	 * @param operationSubmitter How to handle request to submit operation when the queue is full
	 *
	 * @return A {@link CompletableFuture} that will hold an execution report when all the works are complete.
	 * The future will be completed normally even if a work failed,
	 * but the report will contain an exception.
	 */
	CompletableFuture<MultiEntityOperationExecutionReport> executeAndReport(OperationSubmitter operationSubmitter);

	/**
	 * Discard all works that are present in this plan.
	 */
	void discard();
}
