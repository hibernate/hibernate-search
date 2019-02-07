/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.index.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.backend.index.spi.IndexWorkPlan;
import org.hibernate.search.engine.backend.index.spi.DocumentContributor;
import org.hibernate.search.engine.backend.index.spi.DocumentReferenceProvider;
import org.hibernate.search.backend.lucene.document.impl.LuceneIndexEntry;
import org.hibernate.search.backend.lucene.document.impl.LuceneRootDocumentBuilder;
import org.hibernate.search.backend.lucene.multitenancy.impl.MultiTenancyStrategy;
import org.hibernate.search.backend.lucene.orchestration.impl.LuceneIndexWorkOrchestrator;
import org.hibernate.search.backend.lucene.work.impl.LuceneIndexWork;
import org.hibernate.search.backend.lucene.work.impl.LuceneWorkFactory;
import org.hibernate.search.engine.mapper.session.context.spi.SessionContextImplementor;


/**
 * @author Guillaume Smet
 */
class LuceneIndexWorkPlan implements IndexWorkPlan<LuceneRootDocumentBuilder> {

	private final LuceneWorkFactory factory;
	private final MultiTenancyStrategy multiTenancyStrategy;
	private final LuceneIndexWorkOrchestrator orchestrator;
	private final String indexName;
	private final String tenantId;

	private final List<LuceneIndexWork<?>> works = new ArrayList<>();

	LuceneIndexWorkPlan(LuceneWorkFactory factory, MultiTenancyStrategy multiTenancyStrategy,
			LuceneIndexWorkOrchestrator orchestrator,
			String indexName, SessionContextImplementor sessionContext) {
		this.factory = factory;
		this.multiTenancyStrategy = multiTenancyStrategy;
		this.orchestrator = orchestrator;
		this.indexName = indexName;
		this.tenantId = sessionContext.getTenantIdentifier();
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
		// FIXME remove this explicit commit
		collect( factory.commit( indexName ) );
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
		// FIXME remove this explicit commit
		collect( factory.commit( indexName ) );
	}

	@Override
	public void delete(DocumentReferenceProvider referenceProvider) {
		String id = referenceProvider.getIdentifier();
		String routingKey = referenceProvider.getRoutingKey();

		collect( factory.delete( indexName, tenantId, id, routingKey ) );
		// FIXME remove this explicit commit
		collect( factory.commit( indexName ) );
	}

	@Override
	public void prepare() {
		// Nothing to do: we only have to send the works to the orchestrator
	}

	@Override
	public CompletableFuture<?> execute() {
		try {
			return orchestrator.submit( works );
		}
		finally {
			works.clear();
		}
	}

	private void collect(LuceneIndexWork<?> work) {
		works.add( work );
	}
}
