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
import org.hibernate.search.backend.lucene.document.impl.LuceneRootDocumentBuilder;
import org.hibernate.search.backend.lucene.orchestration.impl.LuceneWriteWorkOrchestrator;
import org.hibernate.search.backend.lucene.work.impl.LuceneWorkFactory;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.backend.work.execution.spi.DocumentContributor;
import org.hibernate.search.engine.backend.work.execution.spi.DocumentReferenceProvider;
import org.hibernate.search.engine.backend.work.execution.spi.IndexDocumentWorkExecutor;
import org.hibernate.search.engine.mapper.session.context.spi.SessionContextImplementor;

public class LuceneIndexDocumentWorkExecutor implements IndexDocumentWorkExecutor<LuceneRootDocumentBuilder> {

	private final LuceneWorkFactory factory;
	private final LuceneIndexEntryFactory indexEntryFactory;
	private final WorkExecutionIndexManagerContext indexManagerContext;
	private final String tenantId;
	private final DocumentCommitStrategy commitStrategy;

	public LuceneIndexDocumentWorkExecutor(LuceneWorkFactory factory,
			LuceneIndexEntryFactory indexEntryFactory,
			WorkExecutionIndexManagerContext indexManagerContext,
			SessionContextImplementor sessionContext,
			DocumentCommitStrategy commitStrategy) {
		this.factory = factory;
		this.indexEntryFactory = indexEntryFactory;
		this.indexManagerContext = indexManagerContext;
		this.tenantId = sessionContext.getTenantIdentifier();
		this.commitStrategy = commitStrategy;
	}

	@Override
	public CompletableFuture<?> add(DocumentReferenceProvider referenceProvider, DocumentContributor<LuceneRootDocumentBuilder> documentContributor) {
		String id = referenceProvider.getIdentifier();
		String routingKey = referenceProvider.getRoutingKey();

		LuceneIndexEntry indexEntry = indexEntryFactory.create( tenantId, id, documentContributor );

		// Route the work to the appropriate shard
		LuceneWriteWorkOrchestrator orchestrator = indexManagerContext.getWriteOrchestrator( id, routingKey );

		return orchestrator.submit(
				factory.add( tenantId, id, indexEntry ),
				commitStrategy,
				DocumentRefreshStrategy.NONE
		);
	}
}
