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
import org.hibernate.search.engine.backend.work.execution.spi.DocumentContributor;
import org.hibernate.search.engine.backend.work.execution.spi.DocumentReferenceProvider;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexer;
import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;

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
		this.tenantId = sessionContext.getTenantIdentifier();
	}

	@Override
	public CompletableFuture<?> add(DocumentReferenceProvider referenceProvider, DocumentContributor documentContributor) {
		String id = referenceProvider.getIdentifier();
		String routingKey = referenceProvider.getRoutingKey();

		LuceneIndexEntry indexEntry = indexEntryFactory.create( tenantId, id, routingKey, documentContributor );

		return submit( id, routingKey, factory.add(
				tenantId, indexManagerContext.getMappedTypeName(),
				referenceProvider.getEntityIdentifier(), id,
				indexEntry
		) );
	}

	@Override
	public CompletableFuture<?> update(DocumentReferenceProvider referenceProvider,
			DocumentContributor documentContributor) {
		String id = referenceProvider.getIdentifier();
		String routingKey = referenceProvider.getRoutingKey();

		LuceneIndexEntry indexEntry = indexEntryFactory.create( tenantId, id, routingKey, documentContributor );

		return submit( id, routingKey, factory.update(
				tenantId, indexManagerContext.getMappedTypeName(),
				referenceProvider.getEntityIdentifier(), id,
				indexEntry
		) );
	}

	@Override
	public CompletableFuture<?> delete(DocumentReferenceProvider referenceProvider) {
		String id = referenceProvider.getIdentifier();
		String routingKey = referenceProvider.getRoutingKey();
		return submit( id, routingKey, factory.delete(
				tenantId, indexManagerContext.getMappedTypeName(),
				referenceProvider.getEntityIdentifier(), id
		) );
	}

	private <T> CompletableFuture<T> submit(String documentId, String routingKey, IndexingWork<T> work) {
		// Route the work to the appropriate shard
		LuceneSerialWorkOrchestrator orchestrator = indexManagerContext.getIndexingOrchestrator( documentId, routingKey );

		CompletableFuture<T> future = new CompletableFuture<>();
		orchestrator.submit( future, work );
		return future;
	}
}
