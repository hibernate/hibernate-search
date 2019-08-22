/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.work.execution.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.lucene.document.impl.LuceneIndexEntryFactory;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.backend.work.execution.spi.IndexWorkPlan;
import org.hibernate.search.engine.backend.work.execution.spi.DocumentContributor;
import org.hibernate.search.engine.backend.work.execution.spi.DocumentReferenceProvider;
import org.hibernate.search.backend.lucene.document.impl.LuceneIndexEntry;
import org.hibernate.search.backend.lucene.document.impl.LuceneRootDocumentBuilder;
import org.hibernate.search.backend.lucene.orchestration.impl.LuceneWriteWorkOrchestrator;
import org.hibernate.search.backend.lucene.work.impl.LuceneWriteWork;
import org.hibernate.search.backend.lucene.work.impl.LuceneWorkFactory;
import org.hibernate.search.engine.mapper.session.context.spi.SessionContextImplementor;

public class LuceneIndexWorkPlan implements IndexWorkPlan<LuceneRootDocumentBuilder> {

	private final LuceneWorkFactory factory;
	private final LuceneIndexEntryFactory indexEntryFactory;
	private final WorkExecutionIndexManagerContext indexManagerContext;
	private final String tenantId;
	private final DocumentCommitStrategy commitStrategy;
	private final DocumentRefreshStrategy refreshStrategy;

	private final Map<LuceneWriteWorkOrchestrator, List<LuceneWriteWork<?>>> worksByOrchestrator = new HashMap<>();

	public LuceneIndexWorkPlan(LuceneWorkFactory factory,
			WorkExecutionIndexManagerContext indexManagerContext,
			LuceneIndexEntryFactory indexEntryFactory,
			SessionContextImplementor sessionContext,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy) {
		this.factory = factory;
		this.indexEntryFactory = indexEntryFactory;
		this.indexManagerContext = indexManagerContext;
		this.tenantId = sessionContext.getTenantIdentifier();
		this.commitStrategy = commitStrategy;
		this.refreshStrategy = refreshStrategy;
	}

	@Override
	public void add(DocumentReferenceProvider referenceProvider,
			DocumentContributor<LuceneRootDocumentBuilder> documentContributor) {
		String id = referenceProvider.getIdentifier();
		String routingKey = referenceProvider.getRoutingKey();

		LuceneIndexEntry indexEntry = indexEntryFactory.create( tenantId, id, documentContributor );

		collect( id, routingKey, factory.add( tenantId, id, indexEntry ) );
	}

	@Override
	public void update(DocumentReferenceProvider referenceProvider,
			DocumentContributor<LuceneRootDocumentBuilder> documentContributor) {
		String id = referenceProvider.getIdentifier();
		String routingKey = referenceProvider.getRoutingKey();

		LuceneIndexEntry indexEntry = indexEntryFactory.create( tenantId, id, documentContributor );

		collect( id, routingKey, factory.update( tenantId, id, indexEntry ) );
	}

	@Override
	public void delete(DocumentReferenceProvider referenceProvider) {
		String id = referenceProvider.getIdentifier();
		String routingKey = referenceProvider.getRoutingKey();

		collect( id, routingKey, factory.delete( tenantId, id ) );
	}

	@Override
	public void prepare() {
		// Nothing to do: we only have to send the works to the orchestrator
	}

	@Override
	public CompletableFuture<?> execute() {
		try {
			List<CompletableFuture<?>> futures = new ArrayList<>();
			for ( Map.Entry<LuceneWriteWorkOrchestrator, List<LuceneWriteWork<?>>> entry : worksByOrchestrator.entrySet() ) {
				LuceneWriteWorkOrchestrator orchestrator = entry.getKey();
				List<LuceneWriteWork<?>> works = entry.getValue();
				futures.add( orchestrator.submit( works, commitStrategy, refreshStrategy ) );
			}
			return CompletableFuture.allOf( futures.toArray( new CompletableFuture<?>[0] ) );
		}
		finally {
			worksByOrchestrator.clear();
		}
	}

	@Override
	public void discard() {
		worksByOrchestrator.clear();
	}

	private void collect(String documentId, String routingKey, LuceneWriteWork<?> work) {
		// Route the work to the appropriate shard
		LuceneWriteWorkOrchestrator orchestrator = indexManagerContext.getWriteOrchestrator( documentId, routingKey );

		List<LuceneWriteWork<?>> works = worksByOrchestrator.get( orchestrator );
		if ( works == null ) {
			works = new ArrayList<>();
			worksByOrchestrator.put( orchestrator, works );
		}
		works.add( work );
	}
}
