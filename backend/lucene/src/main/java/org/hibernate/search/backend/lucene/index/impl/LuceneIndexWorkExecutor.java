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

import org.hibernate.search.backend.lucene.document.impl.LuceneIndexEntry;
import org.hibernate.search.backend.lucene.document.impl.LuceneRootDocumentBuilder;
import org.hibernate.search.backend.lucene.multitenancy.impl.MultiTenancyStrategy;
import org.hibernate.search.backend.lucene.orchestration.impl.LuceneIndexWorkOrchestrator;
import org.hibernate.search.backend.lucene.work.impl.LuceneIndexWork;
import org.hibernate.search.backend.lucene.work.impl.LuceneWorkFactory;
import org.hibernate.search.engine.backend.index.spi.DocumentContributor;
import org.hibernate.search.engine.backend.index.spi.DocumentReferenceProvider;
import org.hibernate.search.engine.backend.index.spi.IndexWorkExecutor;
import org.hibernate.search.engine.mapper.session.context.spi.SessionContextImplementor;

class LuceneIndexWorkExecutor implements IndexWorkExecutor<LuceneRootDocumentBuilder> {

	private final LuceneWorkFactory factory;
	private final MultiTenancyStrategy multiTenancyStrategy;
	private final LuceneIndexWorkOrchestrator orchestrator;
	private final String indexName;
	private final String tenantId;

	LuceneIndexWorkExecutor(LuceneWorkFactory factory, MultiTenancyStrategy multiTenancyStrategy,
			LuceneIndexWorkOrchestrator orchestrator,
			String indexName, SessionContextImplementor sessionContext) {
		this.factory = factory;
		this.multiTenancyStrategy = multiTenancyStrategy;
		this.orchestrator = orchestrator;
		this.indexName = indexName;
		this.tenantId = sessionContext.getTenantIdentifier();
	}

	@Override
	public CompletableFuture<?> add(DocumentReferenceProvider referenceProvider, DocumentContributor<LuceneRootDocumentBuilder> documentContributor) {
		String id = referenceProvider.getIdentifier();
		String routingKey = referenceProvider.getRoutingKey();

		LuceneRootDocumentBuilder builder = new LuceneRootDocumentBuilder();
		documentContributor.contribute( builder );
		LuceneIndexEntry indexEntry = builder.build( indexName, multiTenancyStrategy, tenantId, id );

		List<LuceneIndexWork<?>> works = new ArrayList<>();
		works.add( factory.add( indexName, tenantId, id, routingKey, indexEntry ) );
		// FIXME remove this explicit commit
		works.add( factory.commit( indexName ) );

		return orchestrator.submit( works );
	}
}
