/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.index.impl;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchClient;
import org.hibernate.search.backend.elasticsearch.gson.spi.GsonProvider;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.document.impl.ElasticsearchDocumentObjectBuilder;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexModel;
import org.hibernate.search.backend.elasticsearch.multitenancy.impl.MultiTenancyStrategy;
import org.hibernate.search.backend.elasticsearch.orchestration.impl.ElasticsearchWorkOrchestrator;
import org.hibernate.search.backend.elasticsearch.orchestration.impl.ElasticsearchStubWorkOrchestrator;
import org.hibernate.search.backend.elasticsearch.work.builder.factory.impl.ElasticsearchWorkBuilderFactory;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchWork;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchWorkFactory;
import org.hibernate.search.engine.backend.index.spi.IndexWorkExecutor;
import org.hibernate.search.engine.backend.index.spi.IndexDocumentWorkExecutor;
import org.hibernate.search.engine.backend.index.spi.IndexWorkPlan;
import org.hibernate.search.engine.mapper.session.context.spi.SessionContextImplementor;
import org.hibernate.search.util.EventContext;

public class IndexingBackendContext {
	private final EventContext eventContext;

	private final ElasticsearchClient client;
	private final GsonProvider gsonProvider;
	private final ElasticsearchWorkFactory workFactory;
	private final ElasticsearchWorkBuilderFactory workBuilderFactory;
	private final MultiTenancyStrategy multiTenancyStrategy;

	private final ElasticsearchWorkOrchestrator streamOrchestrator;

	public IndexingBackendContext(EventContext eventContext,
			ElasticsearchClient client, GsonProvider gsonProvider,
			ElasticsearchWorkFactory workFactory, ElasticsearchWorkBuilderFactory workBuilderFactory,
			MultiTenancyStrategy multiTenancyStrategy,
			ElasticsearchWorkOrchestrator streamOrchestrator) {
		this.eventContext = eventContext;
		this.client = client;
		this.gsonProvider = gsonProvider;
		this.multiTenancyStrategy = multiTenancyStrategy;
		this.workFactory = workFactory;
		this.workBuilderFactory = workBuilderFactory;
		this.streamOrchestrator = streamOrchestrator;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + eventContext + "]";
	}

	EventContext getEventContext() {
		return eventContext;
	}

	CompletableFuture<?> initializeIndex(URLEncodedString indexName, URLEncodedString typeName,
			ElasticsearchIndexModel model) {
		ElasticsearchWork<?> dropWork = workFactory.dropIndexIfExists( indexName );
		ElasticsearchWork<?> createWork = workFactory.createIndex(
				indexName, typeName,
				model.getMapping(), model.getSettings()
		);
		return streamOrchestrator.submit( Arrays.asList( dropWork, createWork ) );
	}

	ElasticsearchWorkOrchestrator createWorkPlanOrchestrator() {
		return new ElasticsearchStubWorkOrchestrator( client, gsonProvider );
	}

	IndexWorkPlan<ElasticsearchDocumentObjectBuilder> createWorkPlan(
			ElasticsearchWorkOrchestrator orchestrator,
			URLEncodedString indexName, URLEncodedString typeName,
			SessionContextImplementor sessionContext) {
		multiTenancyStrategy.checkTenantId( sessionContext.getTenantIdentifier(), eventContext );

		return new ElasticsearchIndexWorkPlan( workFactory, multiTenancyStrategy, workBuilderFactory, orchestrator,
				indexName, typeName, sessionContext );
	}

	IndexDocumentWorkExecutor<ElasticsearchDocumentObjectBuilder> createDocumentWorkExecutor(
			ElasticsearchWorkOrchestrator orchestrator,
			URLEncodedString indexName, URLEncodedString typeName,
			SessionContextImplementor sessionContext) {
		multiTenancyStrategy.checkTenantId( sessionContext.getTenantIdentifier(), eventContext );

		return new ElasticsearchIndexDocumentWorkExecutor( workBuilderFactory, multiTenancyStrategy, orchestrator,
				indexName, typeName, sessionContext );
	}

	IndexWorkExecutor createWorkExecutor(URLEncodedString indexName) {
		return new ElasticsearchIndexWorkExecutor( workBuilderFactory, multiTenancyStrategy, streamOrchestrator, indexName, eventContext );
	}
}
