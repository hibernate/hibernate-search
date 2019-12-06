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

import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.document.impl.ElasticsearchDocumentObjectBuilder;
import org.hibernate.search.backend.elasticsearch.orchestration.impl.ElasticsearchWorkOrchestrator;
import org.hibernate.search.backend.elasticsearch.work.builder.factory.impl.ElasticsearchWorkBuilderFactory;
import org.hibernate.search.backend.elasticsearch.work.impl.SingleDocumentElasticsearchWork;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlan;
import org.hibernate.search.engine.backend.work.execution.spi.DocumentContributor;
import org.hibernate.search.engine.backend.work.execution.spi.DocumentReferenceProvider;
import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlanExecutionReport;

import com.google.gson.JsonObject;



public class ElasticsearchIndexIndexingPlan implements IndexIndexingPlan<ElasticsearchDocumentObjectBuilder> {

	private final ElasticsearchWorkBuilderFactory builderFactory;
	private final ElasticsearchWorkOrchestrator orchestrator;
	private final WorkExecutionIndexManagerContext indexManagerContext;
	private final DocumentRefreshStrategy refreshStrategy;
	private final String tenantId;

	private final List<SingleDocumentElasticsearchWork<?>> works = new ArrayList<>();

	public ElasticsearchIndexIndexingPlan(ElasticsearchWorkBuilderFactory builderFactory,
			ElasticsearchWorkOrchestrator orchestrator,
			WorkExecutionIndexManagerContext indexManagerContext,
			DocumentRefreshStrategy refreshStrategy,
			BackendSessionContext sessionContext) {
		this.builderFactory = builderFactory;
		this.orchestrator = orchestrator;
		this.indexManagerContext = indexManagerContext;
		this.refreshStrategy = refreshStrategy;
		this.tenantId = sessionContext.getTenantIdentifier();
	}

	@Override
	public void add(DocumentReferenceProvider referenceProvider,
			DocumentContributor<ElasticsearchDocumentObjectBuilder> documentContributor) {
		index( referenceProvider, documentContributor );
	}

	@Override
	public void update(DocumentReferenceProvider referenceProvider,
			DocumentContributor<ElasticsearchDocumentObjectBuilder> documentContributor) {
		index( referenceProvider, documentContributor );
	}

	@Override
	public void delete(DocumentReferenceProvider referenceProvider) {
		String elasticsearchId = indexManagerContext.toElasticsearchId( tenantId, referenceProvider.getIdentifier() );
		String routingKey = referenceProvider.getRoutingKey();

		collect(
				builderFactory.delete(
						indexManagerContext.getHibernateSearchIndexName(),
						indexManagerContext.getElasticsearchIndexName(),
						URLEncodedString.fromString( elasticsearchId ), routingKey
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
	public CompletableFuture<IndexIndexingPlanExecutionReport> executeAndReport() {
		try {
			CompletableFuture<IndexIndexingPlanExecutionReport> future = new CompletableFuture<>();
			orchestrator.submit( new ElasticsearchIndexingPlanWorkSet( works, future ) );
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
			DocumentContributor<ElasticsearchDocumentObjectBuilder> documentContributor) {
		String id = referenceProvider.getIdentifier();
		String elasticsearchId = indexManagerContext.toElasticsearchId( tenantId, id );
		String routingKey = referenceProvider.getRoutingKey();

		JsonObject document = indexManagerContext.createDocument( tenantId, id, documentContributor );

		collect(
				builderFactory.index(
						indexManagerContext.getHibernateSearchIndexName(),
						indexManagerContext.getElasticsearchIndexName(),
						URLEncodedString.fromString( elasticsearchId ), routingKey, document
				)
						.refresh( refreshStrategy )
						.build()
		);
	}

	private void collect(SingleDocumentElasticsearchWork<?> work) {
		works.add( work );
	}

}
