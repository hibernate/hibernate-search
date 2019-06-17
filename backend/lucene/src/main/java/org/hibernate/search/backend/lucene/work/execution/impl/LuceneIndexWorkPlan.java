/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.work.execution.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.backend.work.execution.spi.IndexWorkPlan;
import org.hibernate.search.engine.backend.work.execution.spi.DocumentContributor;
import org.hibernate.search.engine.backend.work.execution.spi.DocumentReferenceProvider;
import org.hibernate.search.backend.lucene.document.impl.LuceneIndexEntry;
import org.hibernate.search.backend.lucene.document.impl.LuceneRootDocumentBuilder;
import org.hibernate.search.backend.lucene.multitenancy.impl.MultiTenancyStrategy;
import org.hibernate.search.backend.lucene.orchestration.impl.LuceneWriteWorkOrchestrator;
import org.hibernate.search.backend.lucene.work.impl.LuceneWriteWork;
import org.hibernate.search.backend.lucene.work.impl.LuceneWorkFactory;
import org.hibernate.search.engine.mapper.session.context.spi.SessionContextImplementor;



public class LuceneIndexWorkPlan implements IndexWorkPlan<LuceneRootDocumentBuilder> {

	private final LuceneWorkFactory factory;
	private final MultiTenancyStrategy multiTenancyStrategy;
	private final LuceneWriteWorkOrchestrator orchestrator;
	private final String indexName;
	private final String tenantId;
	private final DocumentCommitStrategy commitStrategy;
	private final DocumentRefreshStrategy refreshStrategy;

	private final List<LuceneWriteWork<?>> works = new ArrayList<>();

	public LuceneIndexWorkPlan(LuceneWorkFactory factory, MultiTenancyStrategy multiTenancyStrategy,
			LuceneWriteWorkOrchestrator orchestrator,
			String indexName, SessionContextImplementor sessionContext,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy) {
		this.factory = factory;
		this.multiTenancyStrategy = multiTenancyStrategy;
		this.orchestrator = orchestrator;
		this.indexName = indexName;
		this.tenantId = sessionContext.getTenantIdentifier();
		this.commitStrategy = commitStrategy;
		this.refreshStrategy = refreshStrategy;
	}

	@Override
	public void add(DocumentReferenceProvider referenceProvider,
			DocumentContributor<LuceneRootDocumentBuilder> documentContributor) {
		String id = referenceProvider.getIdentifier();
		String routingKey = referenceProvider.getRoutingKey();

		LuceneRootDocumentBuilder builder = new LuceneRootDocumentBuilder();
		documentContributor.contribute( builder );
		LuceneIndexEntry indexEntry = builder.build( indexName, multiTenancyStrategy, tenantId, id );

		collect( factory.add( indexName, tenantId, id, routingKey, indexEntry ) );
	}

	@Override
	public void update(DocumentReferenceProvider referenceProvider,
			DocumentContributor<LuceneRootDocumentBuilder> documentContributor) {
		String id = referenceProvider.getIdentifier();
		String routingKey = referenceProvider.getRoutingKey();

		LuceneRootDocumentBuilder builder = new LuceneRootDocumentBuilder();
		documentContributor.contribute( builder );
		LuceneIndexEntry indexEntry = builder.build( indexName, multiTenancyStrategy, tenantId, id );

		collect( factory.update( indexName, tenantId, id, routingKey, indexEntry ) );
	}

	@Override
	public void delete(DocumentReferenceProvider referenceProvider) {
		String id = referenceProvider.getIdentifier();
		String routingKey = referenceProvider.getRoutingKey();

		collect( factory.delete( indexName, tenantId, id, routingKey ) );
	}

	@Override
	public void prepare() {
		// Nothing to do: we only have to send the works to the orchestrator
	}

	@Override
	public CompletableFuture<?> execute() {
		try {
			return orchestrator.submit( works, commitStrategy, refreshStrategy );
		}
		finally {
			works.clear();
		}
	}

	private void collect(LuceneWriteWork<?> work) {
		works.add( work );
	}
}
