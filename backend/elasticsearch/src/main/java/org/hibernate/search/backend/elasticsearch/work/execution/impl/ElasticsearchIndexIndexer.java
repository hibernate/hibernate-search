/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.work.execution.impl;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.elasticsearch.orchestration.impl.ElasticsearchSerialWorkOrchestrator;
import org.hibernate.search.backend.elasticsearch.work.factory.impl.ElasticsearchWorkFactory;
import org.hibernate.search.backend.elasticsearch.work.impl.SingleDocumentIndexingWork;
import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.engine.backend.work.execution.spi.DocumentContributor;
import org.hibernate.search.engine.backend.work.execution.spi.DocumentReferenceProvider;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexer;

import com.google.gson.JsonObject;

public class ElasticsearchIndexIndexer implements IndexIndexer {

	private final ElasticsearchWorkFactory workFactory;
	private final ElasticsearchSerialWorkOrchestrator orchestrator;
	private final WorkExecutionIndexManagerContext indexManagerContext;
	private final String tenantId;

	public ElasticsearchIndexIndexer(ElasticsearchWorkFactory workFactory,
			ElasticsearchSerialWorkOrchestrator orchestrator,
			WorkExecutionIndexManagerContext indexManagerContext,
			BackendSessionContext sessionContext) {
		this.workFactory = workFactory;
		this.orchestrator = orchestrator;
		this.indexManagerContext = indexManagerContext;
		this.tenantId = sessionContext.tenantIdentifier();
	}

	@Override
	public CompletableFuture<?> add(DocumentReferenceProvider referenceProvider,
			DocumentContributor documentContributor,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy,
			OperationSubmitter operationSubmitter) {
		return index( referenceProvider, documentContributor, refreshStrategy, operationSubmitter );
	}

	@Override
	public CompletableFuture<?> addOrUpdate(DocumentReferenceProvider referenceProvider,
			DocumentContributor documentContributor,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy,
			OperationSubmitter operationSubmitter) {
		return index( referenceProvider, documentContributor, refreshStrategy, operationSubmitter );
	}

	@Override
	public CompletableFuture<?> delete(DocumentReferenceProvider referenceProvider,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy,
			OperationSubmitter operationSubmitter) {
		String id = referenceProvider.identifier();
		String elasticsearchId = indexManagerContext.toElasticsearchId( tenantId, id );
		String routingKey = referenceProvider.routingKey();

		SingleDocumentIndexingWork work = workFactory.delete(
				indexManagerContext.getMappedTypeName(), referenceProvider.entityIdentifier(),
				indexManagerContext.getElasticsearchIndexWriteName(),
				elasticsearchId, routingKey
		)
				// The commit strategy is ignored, because Elasticsearch always commits changes to its transaction log.
				.refresh( refreshStrategy )
				.build();
		return orchestrator.submit( work, operationSubmitter );
	}

	private CompletableFuture<?> index(DocumentReferenceProvider referenceProvider,
			DocumentContributor documentContributor,
			DocumentRefreshStrategy refreshStrategy,
			OperationSubmitter operationSubmitter) {
		String id = referenceProvider.identifier();
		String elasticsearchId = indexManagerContext.toElasticsearchId( tenantId, id );
		String routingKey = referenceProvider.routingKey();

		JsonObject document = indexManagerContext.createDocument( tenantId, id, documentContributor );

		SingleDocumentIndexingWork work = workFactory.index(
				indexManagerContext.getMappedTypeName(), referenceProvider.entityIdentifier(),
				indexManagerContext.getElasticsearchIndexWriteName(),
				elasticsearchId, routingKey, document
		)
				// The commit strategy is ignored, because Elasticsearch always commits changes to its transaction log.
				.refresh( refreshStrategy )
				.build();
		return orchestrator.submit( work, operationSubmitter );
	}
}
