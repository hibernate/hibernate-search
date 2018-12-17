/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.index.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.document.impl.ElasticsearchDocumentObjectBuilder;
import org.hibernate.search.backend.elasticsearch.multitenancy.impl.MultiTenancyStrategy;
import org.hibernate.search.backend.elasticsearch.orchestration.impl.ElasticsearchWorkOrchestrator;
import org.hibernate.search.backend.elasticsearch.work.builder.factory.impl.ElasticsearchWorkBuilderFactory;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchWork;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchWorkFactory;
import org.hibernate.search.engine.backend.index.spi.IndexWorkPlan;
import org.hibernate.search.engine.backend.index.spi.DocumentContributor;
import org.hibernate.search.engine.backend.index.spi.DocumentReferenceProvider;
import org.hibernate.search.engine.mapper.session.context.spi.SessionContextImplementor;

import com.google.gson.JsonObject;


/**
 * @author Yoann Rodiere
 */
public class ElasticsearchIndexWorkPlan implements IndexWorkPlan<ElasticsearchDocumentObjectBuilder> {

	private final ElasticsearchWorkFactory factory;
	private final MultiTenancyStrategy multiTenancyStrategy;
	private final ElasticsearchWorkBuilderFactory builderFactory;
	private final ElasticsearchWorkOrchestrator orchestrator;
	private final URLEncodedString indexName;
	private final URLEncodedString typeName;
	private final String tenantId;

	private final List<ElasticsearchWork<?>> works = new ArrayList<>();

	ElasticsearchIndexWorkPlan(ElasticsearchWorkFactory factory, MultiTenancyStrategy multiTenancyStrategy,
			ElasticsearchWorkBuilderFactory builderFactory,
			ElasticsearchWorkOrchestrator orchestrator,
			URLEncodedString indexName, URLEncodedString typeName,
			SessionContextImplementor sessionContext) {
		this.factory = factory;
		this.multiTenancyStrategy = multiTenancyStrategy;
		this.builderFactory = builderFactory;
		this.orchestrator = orchestrator;
		this.indexName = indexName;
		this.typeName = typeName;
		this.tenantId = sessionContext.getTenantIdentifier();
	}

	@Override
	public void add(DocumentReferenceProvider referenceProvider,
			DocumentContributor<ElasticsearchDocumentObjectBuilder> documentContributor) {
		String id = referenceProvider.getIdentifier();
		String elasticsearchId = multiTenancyStrategy.toElasticsearchId( tenantId, id );
		String routingKey = referenceProvider.getRoutingKey();

		ElasticsearchDocumentObjectBuilder builder = new ElasticsearchDocumentObjectBuilder();
		documentContributor.contribute( builder );
		JsonObject document = builder.build( multiTenancyStrategy, tenantId, id );

		collect( builderFactory.index( indexName, typeName, URLEncodedString.fromString( elasticsearchId ), routingKey, document ).build() );
	}

	@Override
	public void update(DocumentReferenceProvider referenceProvider,
			DocumentContributor<ElasticsearchDocumentObjectBuilder> documentContributor) {
		String id = referenceProvider.getIdentifier();
		String elasticsearchId = multiTenancyStrategy.toElasticsearchId( tenantId, id );
		String routingKey = referenceProvider.getRoutingKey();

		ElasticsearchDocumentObjectBuilder builder = new ElasticsearchDocumentObjectBuilder();
		documentContributor.contribute( builder );
		JsonObject document = builder.build( multiTenancyStrategy, tenantId, id );

		collect( factory.update( indexName, typeName, elasticsearchId, routingKey, document ) );
	}

	@Override
	public void delete(DocumentReferenceProvider referenceProvider) {
		String elasticsearchId = multiTenancyStrategy.toElasticsearchId( tenantId, referenceProvider.getIdentifier() );
		String routingKey = referenceProvider.getRoutingKey();

		collect( factory.delete( indexName, typeName, elasticsearchId, routingKey ) );
	}

	@Override
	public void prepare() {
		/*
		 * Nothing to do: we can't execute anything more
		 * without sending a request to the cluster.
		 */
	}

	@Override
	public CompletableFuture<?> execute() {
		try {
			CompletableFuture<?> future = orchestrator.submit( works );
			return future;
		}
		finally {
			works.clear();
		}
	}

	private void collect(ElasticsearchWork<?> work) {
		works.add( work );
	}

}
