/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.work.execution.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.elasticsearch.orchestration.impl.ElasticsearchSerialWorkOrchestrator;
import org.hibernate.search.backend.elasticsearch.work.factory.impl.ElasticsearchWorkFactory;
import org.hibernate.search.backend.elasticsearch.work.impl.SingleDocumentIndexingWork;
import org.hibernate.search.engine.backend.common.spi.EntityReferenceFactory;
import org.hibernate.search.engine.backend.common.spi.MultiEntityOperationExecutionReport;
import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.engine.backend.work.execution.spi.DocumentContributor;
import org.hibernate.search.engine.backend.work.execution.spi.DocumentReferenceProvider;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlan;

import com.google.gson.JsonObject;

public class ElasticsearchIndexIndexingPlan implements IndexIndexingPlan {

	private final ElasticsearchWorkFactory workFactory;
	private final ElasticsearchSerialWorkOrchestrator orchestrator;
	private final WorkExecutionIndexManagerContext indexManagerContext;
	private final EntityReferenceFactory entityReferenceFactory;
	private final String tenantId;
	private final DocumentRefreshStrategy refreshStrategy;

	private final List<SingleDocumentIndexingWork> works = new ArrayList<>();

	public ElasticsearchIndexIndexingPlan(ElasticsearchWorkFactory workFactory,
			ElasticsearchSerialWorkOrchestrator orchestrator,
			WorkExecutionIndexManagerContext indexManagerContext,
			BackendSessionContext sessionContext,
			DocumentRefreshStrategy refreshStrategy) {
		this.workFactory = workFactory;
		this.orchestrator = orchestrator;
		this.indexManagerContext = indexManagerContext;
		this.entityReferenceFactory = sessionContext.mappingContext().entityReferenceFactory();
		this.tenantId = sessionContext.tenantIdentifier();
		this.refreshStrategy = refreshStrategy;
	}

	@Override
	public void add(DocumentReferenceProvider referenceProvider,
			DocumentContributor documentContributor) {
		index( referenceProvider, documentContributor );
	}

	@Override
	public void addOrUpdate(DocumentReferenceProvider referenceProvider,
			DocumentContributor documentContributor) {
		index( referenceProvider, documentContributor );
	}

	@Override
	public void delete(DocumentReferenceProvider referenceProvider) {
		String elasticsearchId = indexManagerContext.toElasticsearchId( tenantId, referenceProvider.identifier() );
		String routingKey = referenceProvider.routingKey();

		collect(
				workFactory.delete(
						indexManagerContext.getMappedTypeName(), referenceProvider.entityIdentifier(),
						indexManagerContext.getElasticsearchIndexWriteName(),
						elasticsearchId, routingKey
				)
						.refresh( refreshStrategy )
						.build()
		);
	}

	@Override
	public CompletableFuture<MultiEntityOperationExecutionReport> executeAndReport(OperationSubmitter operationSubmitter) {
		try {
			ElasticsearchIndexIndexingPlanExecution execution = new ElasticsearchIndexIndexingPlanExecution(
					orchestrator, entityReferenceFactory,
					new ArrayList<>( works ) // Copy the list, as we're going to clear it below
			);
			return execution.execute( operationSubmitter );
		}
		finally {
			works.clear();
		}
	}

	@Override
	public void discard() {
		works.clear();
	}

	private void index(DocumentReferenceProvider referenceProvider,
			DocumentContributor documentContributor) {
		String id = referenceProvider.identifier();
		String elasticsearchId = indexManagerContext.toElasticsearchId( tenantId, id );
		String routingKey = referenceProvider.routingKey();

		JsonObject document = indexManagerContext.createDocument( tenantId, id, documentContributor );

		collect(
				workFactory.index(
						indexManagerContext.getMappedTypeName(), referenceProvider.entityIdentifier(),
						indexManagerContext.getElasticsearchIndexWriteName(),
						elasticsearchId, routingKey, document
				)
						.refresh( refreshStrategy )
						.build()
		);
	}

	private void collect(SingleDocumentIndexingWork work) {
		works.add( work );
	}

}
