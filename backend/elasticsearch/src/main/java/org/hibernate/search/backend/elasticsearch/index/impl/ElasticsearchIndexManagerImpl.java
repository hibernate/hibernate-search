/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.index.impl;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.elasticsearch.ElasticsearchBackend;
import org.hibernate.search.backend.elasticsearch.document.impl.DocumentMetadataContributor;
import org.hibernate.search.backend.elasticsearch.document.impl.ElasticsearchDocumentObjectBuilder;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexModel;
import org.hibernate.search.backend.elasticsearch.index.ElasticsearchIndexManager;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.metamodel.ElasticsearchIndexDescriptor;
import org.hibernate.search.backend.elasticsearch.orchestration.impl.ElasticsearchBatchingWorkOrchestrator;
import org.hibernate.search.backend.elasticsearch.schema.management.impl.ElasticsearchIndexSchemaManager;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.work.execution.impl.WorkExecutionIndexManagerContext;
import org.hibernate.search.engine.backend.index.IndexManager;
import org.hibernate.search.engine.backend.index.spi.IndexManagerImplementor;
import org.hibernate.search.engine.backend.index.spi.IndexManagerStartContext;
import org.hibernate.search.engine.backend.mapping.spi.BackendMappingContext;
import org.hibernate.search.engine.backend.schema.management.spi.IndexSchemaManager;
import org.hibernate.search.engine.backend.scope.spi.IndexScopeBuilder;
import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.backend.work.execution.spi.DocumentContributor;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexer;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlan;
import org.hibernate.search.engine.backend.work.execution.spi.IndexWorkspace;
import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.cfg.spi.OptionalConfigurationProperty;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.impl.SuppressingCloser;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reporting.EventContext;

import com.google.gson.JsonObject;

class ElasticsearchIndexManagerImpl
		implements IndexManagerImplementor,
		ElasticsearchIndexManager, WorkExecutionIndexManagerContext {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final OptionalConfigurationProperty<String> OBSOLETE_LIFECYCLE_STRATEGY =
			ConfigurationProperty.forKey( "lifecycle.strategy" )
					.asString()
					.build();

	private final IndexManagerBackendContext backendContext;

	private final ElasticsearchIndexModel model;
	private final List<DocumentMetadataContributor> documentMetadataContributors;

	private final ElasticsearchBatchingWorkOrchestrator indexingOrchestrator;

	private ElasticsearchIndexSchemaManager schemaManager;

	ElasticsearchIndexManagerImpl(IndexManagerBackendContext backendContext,
			ElasticsearchIndexModel model,
			List<DocumentMetadataContributor> documentMetadataContributors) {
		this.backendContext = backendContext;
		this.model = model;
		this.documentMetadataContributors = documentMetadataContributors;
		this.indexingOrchestrator = backendContext.createIndexingOrchestrator( model.hibernateSearchName() );
	}

	@Override
	public String toString() {
		return new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "names=" ).append( model.names() )
				.append( "]" )
				.toString();
	}

	@Override
	public void start(IndexManagerStartContext context) {
		try {
			/*
			 * Create the initializer and lifecycle strategy late to allow the behavior to change
			 * after the first phase of bootstrap,
			 * based on runtime data such as runtime user configuration or the detected ES version.
			 * Useful for compile-time boot.
			 */
			schemaManager = backendContext.createSchemaManager(
					model, context.configurationPropertySource()
			);

			// HSEARCH-3759: the lifecycle strategy is now the schema management strategy, at the mapper level
			OBSOLETE_LIFECYCLE_STRATEGY.getAndMap(
					context.configurationPropertySource(),
					ignored -> {
						throw log.lifecycleStrategyMovedToMapper();
					}
			);

			indexingOrchestrator.start( context.configurationPropertySource() );
		}
		catch (RuntimeException e) {
			new SuppressingCloser( e )
					.push( ElasticsearchBatchingWorkOrchestrator::stop, indexingOrchestrator );
			throw e;
		}
	}

	@Override
	public CompletableFuture<?> preStop() {
		return indexingOrchestrator.preStop();
	}

	@Override
	public void stop() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.push( ElasticsearchBatchingWorkOrchestrator::stop, indexingOrchestrator );
			schemaManager = null;
		}
	}

	@Override
	public String getMappedTypeName() {
		return model.mappedTypeName();
	}

	@Override
	public URLEncodedString getElasticsearchIndexWriteName() {
		return model.names().write();
	}

	@Override
	public String toElasticsearchId(String tenantId, String id) {
		return backendContext.toElasticsearchId( tenantId, id );
	}

	@Override
	public JsonObject createDocument(String tenantId, String id,
			DocumentContributor documentContributor) {
		ElasticsearchDocumentObjectBuilder builder = new ElasticsearchDocumentObjectBuilder( model );
		documentContributor.contribute( builder );
		JsonObject document = builder.build();

		for ( DocumentMetadataContributor contributor : documentMetadataContributors ) {
			contributor.contribute( document, tenantId, id );
		}

		return document;
	}

	public ElasticsearchIndexModel model() {
		return model;
	}

	@Override
	public IndexSchemaManager schemaManager() {
		return schemaManager;
	}

	@Override
	public IndexIndexingPlan createIndexingPlan(BackendSessionContext sessionContext,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy) {
		// The commit strategy is ignored, because Elasticsearch always commits changes to its transaction log.
		return backendContext.createIndexingPlan(
				indexingOrchestrator,
				this,
				sessionContext,
				refreshStrategy
		);
	}

	@Override
	public IndexIndexer createIndexer(BackendSessionContext sessionContext) {
		return backendContext.createIndexer(
				indexingOrchestrator, this, sessionContext
		);
	}

	@Override
	public IndexWorkspace createWorkspace(BackendMappingContext mappingContext, Set<String> tenantIds) {
		return backendContext.createWorkspace( this, tenantIds );
	}

	@Override
	public IndexScopeBuilder createScopeBuilder(BackendMappingContext mappingContext) {
		return new ElasticsearchIndexScopeBuilder(
				backendContext, mappingContext, this
		);
	}

	@Override
	public void addTo(IndexScopeBuilder builder) {
		if ( !( builder instanceof ElasticsearchIndexScopeBuilder ) ) {
			throw log.cannotMixElasticsearchScopeWithOtherType(
					builder, this, backendContext.getEventContext()
			);
		}

		ElasticsearchIndexScopeBuilder esBuilder = (ElasticsearchIndexScopeBuilder) builder;
		esBuilder.add( backendContext, this );
	}

	@Override
	public IndexManager toAPI() {
		return this;
	}

	@Override
	public ElasticsearchBackend backend() {
		return backendContext.toAPI();
	}

	@Override
	public ElasticsearchIndexDescriptor descriptor() {
		return model;
	}

	@Override
	@SuppressWarnings("unchecked") // Checked using reflection
	public <T> T unwrap(Class<T> clazz) {
		if ( clazz.isAssignableFrom( ElasticsearchIndexManager.class ) ) {
			return (T) this;
		}
		throw log.indexManagerUnwrappingWithUnknownType(
				clazz, ElasticsearchIndexManager.class, getBackendAndIndexEventContext()
		);
	}

	private EventContext getBackendAndIndexEventContext() {
		return backendContext.getEventContext().append(
				EventContexts.fromIndexName( model.hibernateSearchName() )
		);
	}

}
