/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.work.execution.impl;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.lucene.document.impl.LuceneIndexEntry;
import org.hibernate.search.backend.lucene.document.impl.LuceneIndexEntryFactory;
import org.hibernate.search.backend.lucene.orchestration.impl.LuceneSerialWorkOrchestrator;
import org.hibernate.search.backend.lucene.work.impl.IndexingWork;
import org.hibernate.search.backend.lucene.work.impl.LuceneWorkFactory;
import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.backend.work.execution.spi.DocumentContributor;
import org.hibernate.search.engine.backend.work.execution.spi.DocumentReferenceProvider;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexer;

public class LuceneIndexIndexer implements IndexIndexer {

	private final LuceneWorkFactory factory;
	private final LuceneIndexEntryFactory indexEntryFactory;
	private final WorkExecutionIndexManagerContext indexManagerContext;
	private final String tenantId;

	public LuceneIndexIndexer(LuceneWorkFactory factory,
			LuceneIndexEntryFactory indexEntryFactory,
			WorkExecutionIndexManagerContext indexManagerContext,
			BackendSessionContext sessionContext) {
		this.factory = factory;
		this.indexEntryFactory = indexEntryFactory;
		this.indexManagerContext = indexManagerContext;
		this.tenantId = sessionContext.tenantIdentifier();
	}

	@Override
	public CompletableFuture<?> add(DocumentReferenceProvider referenceProvider,
			DocumentContributor documentContributor,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy) {
		String id = referenceProvider.identifier();
		String routingKey = referenceProvider.routingKey();

		LuceneIndexEntry indexEntry = indexEntryFactory.create( tenantId, id, routingKey, documentContributor );

		return submit(
				id, routingKey,
				factory.add(
						tenantId, indexManagerContext.getMappedTypeName(),
						referenceProvider.entityIdentifier(), id,
						indexEntry
				),
				commitStrategy, refreshStrategy
		);
	}

	@Override
	public CompletableFuture<?> update(DocumentReferenceProvider referenceProvider,
			DocumentContributor documentContributor,
			DocumentCommitStrategy commitStrategy,
			DocumentRefreshStrategy refreshStrategy) {
		String id = referenceProvider.identifier();
		String routingKey = referenceProvider.routingKey();

		LuceneIndexEntry indexEntry = indexEntryFactory.create( tenantId, id, routingKey, documentContributor );

		return submit(
				id, routingKey,
				factory.update(
						tenantId, indexManagerContext.getMappedTypeName(),
						referenceProvider.entityIdentifier(), id,
						indexEntry
				),
				commitStrategy, refreshStrategy
		);
	}

	@Override
	public CompletableFuture<?> delete(DocumentReferenceProvider referenceProvider,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy) {
		String id = referenceProvider.identifier();
		String routingKey = referenceProvider.routingKey();
		return submit(
				id, routingKey,
				factory.delete(
						tenantId, indexManagerContext.getMappedTypeName(),
						referenceProvider.entityIdentifier(), id
				),
				commitStrategy, refreshStrategy
		);
	}

	private <T> CompletableFuture<T> submit(String documentId, String routingKey, IndexingWork<T> work,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy) {
		// Route the work to the appropriate shard
		LuceneSerialWorkOrchestrator orchestrator = indexManagerContext.getIndexingOrchestrator( documentId, routingKey );

		CompletableFuture<T> futureForOrchestrator = new CompletableFuture<>();
		CompletableFuture<T> futureForCaller;

		boolean needsCommit = DocumentCommitStrategy.FORCE.equals( commitStrategy );
		boolean needsRefresh = DocumentRefreshStrategy.FORCE.equals( refreshStrategy );
		if ( needsCommit || needsRefresh ) {
			// Add the handler to the future *before* submitting the works,
			// so as to be sure that the commit/refresh is executed in the background,
			// not in the current thread.
			// It's important because we don't want to block the current thread.
			futureForCaller = futureForOrchestrator.thenApply( result -> {
				if ( needsCommit ) {
					orchestrator.forceCommitInCurrentThread();
				}
				if ( needsRefresh ) {
					orchestrator.forceRefreshInCurrentThread();
				}
				return result;
			} );
		}
		else {
			futureForCaller = futureForOrchestrator;
		}

		orchestrator.submit( futureForOrchestrator, work );

		return futureForCaller;
	}
}
