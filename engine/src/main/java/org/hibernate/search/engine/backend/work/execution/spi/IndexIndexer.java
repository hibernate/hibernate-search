/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.work.execution.spi;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;

/**
 * An indexer scoped to a single index.
 */
public interface IndexIndexer {

	/**
	 * Add a document to the index, assuming that the document is absent from the index.
	 *
	 * @param referenceProvider A source of information about the identity of the document to add.
	 * @param documentContributor A contributor to the document, adding fields to the indexed document.
	 * @param commitStrategy How to handle the commit.
	 * @param refreshStrategy How to handle the refresh.
	 * @param operationSubmitter How to handle request to submit operation when the queue is full
	 * @return A {@link CompletableFuture} that completes once the document is added.
	 */
	CompletableFuture<?> add(DocumentReferenceProvider referenceProvider, DocumentContributor documentContributor,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy, OperationSubmitter operationSubmitter);

	/**
	 * Update a document in the index, or add it if it's absent from the index.
	 *
	 * @param referenceProvider A source of information about the identity of the document to update.
	 * @param documentContributor A contributor to the document, adding fields to the indexed document.
	 * @param commitStrategy How to handle the commit.
	 * @param refreshStrategy How to handle the refresh.
	 * @param operationSubmitter How to handle request to submit operation when the queue is full
	 * @return A {@link CompletableFuture} that completes once the document is updated.
	 */
	CompletableFuture<?> addOrUpdate(DocumentReferenceProvider referenceProvider, DocumentContributor documentContributor,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy, OperationSubmitter operationSubmitter);

	/**
	 * Delete a document from the index.
	 *
	 * @param referenceProvider A source of information about the identity of the document to delete.
	 * @param commitStrategy How to handle the commit.
	 * @param refreshStrategy How to handle the refresh.
	 * @param operationSubmitter How to handle request to submit operation when the queue is full
	 * @return A {@link CompletableFuture} that completes once the document is deleted.
	 */
	CompletableFuture<?> delete(DocumentReferenceProvider referenceProvider,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy, OperationSubmitter operationSubmitter);

}
