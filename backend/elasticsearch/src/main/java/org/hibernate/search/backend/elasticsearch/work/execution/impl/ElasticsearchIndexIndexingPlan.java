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

import org.hibernate.search.backend.elasticsearch.orchestration.impl.ElasticsearchWorkOrchestrator;
import org.hibernate.search.backend.elasticsearch.work.builder.factory.impl.ElasticsearchWorkBuilderFactory;
import org.hibernate.search.backend.elasticsearch.work.impl.SingleDocumentWork;
import org.hibernate.search.engine.backend.common.spi.EntityReferenceFactory;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlan;
import org.hibernate.search.engine.backend.work.execution.spi.DocumentContributor;
import org.hibernate.search.engine.backend.work.execution.spi.DocumentReferenceProvider;
import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlanExecutionReport;

import com.google.gson.JsonObject;



public class ElasticsearchIndexIndexingPlan<R> implements IndexIndexingPlan<R> {

	private final ElasticsearchWorkBuilderFactory builderFactory;
	private final ElasticsearchWorkOrchestrator orchestrator;
	private final WorkExecutionIndexManagerContext indexManagerContext;
	private final String tenantId;
	private final EntityReferenceFactory<R> entityReferenceFactory;
	private final DocumentRefreshStrategy refreshStrategy;

	private final List<SingleDocumentWork<?>> works = new ArrayList<>();

	public ElasticsearchIndexIndexingPlan(ElasticsearchWorkBuilderFactory builderFactory,
			ElasticsearchWorkOrchestrator orchestrator,
			WorkExecutionIndexManagerContext indexManagerContext,
			BackendSessionContext sessionContext,
			EntityReferenceFactory<R> entityReferenceFactory,
			DocumentRefreshStrategy refreshStrategy) {
		this.builderFactory = builderFactory;
		this.orchestrator = orchestrator;
		this.indexManagerContext = indexManagerContext;
		this.tenantId = sessionContext.getTenantIdentifier();
		this.entityReferenceFactory = entityReferenceFactory;
		this.refreshStrategy = refreshStrategy;
	}

	@Override
	public void add(DocumentReferenceProvider referenceProvider,
			DocumentContributor documentContributor) {
		index( referenceProvider, documentContributor );
	}

	@Override
	public void update(DocumentReferenceProvider referenceProvider,
			DocumentContributor documentContributor) {
		index( referenceProvider, documentContributor );
	}

	@Override
	public void delete(DocumentReferenceProvider referenceProvider) {
		String elasticsearchId = indexManagerContext.toElasticsearchId( tenantId, referenceProvider.getIdentifier() );
		String routingKey = referenceProvider.getRoutingKey();

		collect(
				builderFactory.delete(
						indexManagerContext.getMappedTypeName(), referenceProvider.getEntityIdentifier(),
						indexManagerContext.getElasticsearchIndexWriteName(),
						elasticsearchId, routingKey
				)
						.refresh( refreshStrategy )
						.build()
		);
	}

	@Override
	public void process() {
		/*
		 * Nothing to do: we can't execute anything more
		 * without sending a request to the cluster.
		 */
	}

	@Override
	public CompletableFuture<IndexIndexingPlanExecutionReport<R>> executeAndReport() {
		try {
			CompletableFuture<IndexIndexingPlanExecutionReport<R>> future = new CompletableFuture<>();
			orchestrator.submit( new ElasticsearchIndexingPlanWorkSet<>( works, entityReferenceFactory, future ) );
			return future;
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
		String id = referenceProvider.getIdentifier();
		String elasticsearchId = indexManagerContext.toElasticsearchId( tenantId, id );
		String routingKey = referenceProvider.getRoutingKey();

		JsonObject document = indexManagerContext.createDocument( tenantId, id, documentContributor );

		collect(
				builderFactory.index(
						indexManagerContext.getMappedTypeName(), referenceProvider.getEntityIdentifier(),
						indexManagerContext.getElasticsearchIndexWriteName(),
						elasticsearchId, routingKey, document
				)
						.refresh( refreshStrategy )
						.build()
		);
	}

	private void collect(SingleDocumentWork<?> work) {
		works.add( work );
	}

}
