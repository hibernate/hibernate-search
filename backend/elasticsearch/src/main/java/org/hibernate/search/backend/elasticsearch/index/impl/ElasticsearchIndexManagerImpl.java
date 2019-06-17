/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.index.impl;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexLifecycleStrategyName;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexSettings;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexStatus;
import org.hibernate.search.backend.elasticsearch.document.impl.ElasticsearchDocumentObjectBuilder;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexModel;
import org.hibernate.search.backend.elasticsearch.index.ElasticsearchIndexManager;
import org.hibernate.search.backend.elasticsearch.index.admin.impl.ElasticsearchIndexAdministrationClient;
import org.hibernate.search.backend.elasticsearch.index.admin.impl.ElasticsearchIndexLifecycleExecutionOptions;
import org.hibernate.search.backend.elasticsearch.index.management.impl.ElasticsearchIndexLifecycleStrategy;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.orchestration.impl.ElasticsearchWorkOrchestratorImplementor;
import org.hibernate.search.backend.elasticsearch.search.query.impl.SearchBackendContext;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.index.IndexManager;
import org.hibernate.search.engine.backend.index.spi.IndexManagerStartContext;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.backend.work.execution.spi.IndexWorkExecutor;
import org.hibernate.search.engine.backend.index.spi.IndexManagerImplementor;
import org.hibernate.search.engine.backend.scope.spi.IndexScopeBuilder;
import org.hibernate.search.engine.backend.work.execution.spi.IndexDocumentWorkExecutor;
import org.hibernate.search.engine.backend.work.execution.spi.IndexWorkPlan;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.mapper.session.context.spi.DetachedSessionContextImplementor;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.mapper.mapping.context.spi.MappingContextImplementor;
import org.hibernate.search.engine.mapper.session.context.spi.SessionContextImplementor;
import org.hibernate.search.util.common.impl.SuppressingCloser;
import org.hibernate.search.util.common.reporting.EventContext;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;



class ElasticsearchIndexManagerImpl implements IndexManagerImplementor<ElasticsearchDocumentObjectBuilder>,
		ElasticsearchIndexManager {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final ConfigurationProperty<ElasticsearchIndexLifecycleStrategyName> LIFECYCLE_STRATEGY =
			ConfigurationProperty.forKey( ElasticsearchIndexSettings.LIFECYCLE_STRATEGY )
					.as( ElasticsearchIndexLifecycleStrategyName.class, ElasticsearchIndexLifecycleStrategyName::of )
					.withDefault( ElasticsearchIndexSettings.Defaults.LIFECYCLE_STRATEGY )
					.build();

	private static final ConfigurationProperty<ElasticsearchIndexStatus> MANAGEMENT_REQUIRED_STATUS =
			ConfigurationProperty.forKey( ElasticsearchIndexSettings.LIFECYCLE_MINIMAL_REQUIRED_STATUS )
					.as( ElasticsearchIndexStatus.class, ElasticsearchIndexStatus::of )
					.withDefault( ElasticsearchIndexSettings.Defaults.LIFECYCLE_MINIMAL_REQUIRED_STATUS )
					.build();

	private static final ConfigurationProperty<Integer> MANAGEMENT_REQUIRED_STATUS_WAIT_TIMEOUT =
			ConfigurationProperty.forKey( ElasticsearchIndexSettings.LIFECYCLE_MINIMAL_REQUIRED_STATUS_WAIT_TIMEOUT )
					.asInteger()
					.withDefault( ElasticsearchIndexSettings.Defaults.LIFECYCLE_MINIMAL_REQUIRED_STATUS_WAIT_TIMEOUT )
					.build();

	private final IndexingBackendContext indexingBackendContext;
	private final SearchBackendContext searchBackendContext;

	private final String hibernateSearchIndexName;
	private final URLEncodedString elasticsearchIndexName;
	private final ElasticsearchIndexModel model;

	private final ElasticsearchWorkOrchestratorImplementor serialOrchestrator;
	private final ElasticsearchWorkOrchestratorImplementor parallelOrchestrator;

	private final ElasticsearchIndexAdministrationClient administrationClient;

	private ElasticsearchIndexLifecycleStrategy lifecycleStrategy;

	ElasticsearchIndexManagerImpl(IndexingBackendContext indexingBackendContext, SearchBackendContext searchBackendContext,
			String hibernateSearchIndexName, URLEncodedString elasticsearchIndexName,
			ElasticsearchIndexModel model) {
		this.indexingBackendContext = indexingBackendContext;
		this.searchBackendContext = searchBackendContext;
		this.hibernateSearchIndexName = hibernateSearchIndexName;
		this.elasticsearchIndexName = elasticsearchIndexName;
		this.model = model;
		this.parallelOrchestrator = indexingBackendContext.createParallelOrchestrator( elasticsearchIndexName.original );
		this.serialOrchestrator = indexingBackendContext.createSerialOrchestrator( elasticsearchIndexName.original );
		this.administrationClient = indexingBackendContext.createAdministrationClient(
				elasticsearchIndexName, model
		);
	}

	@Override
	public void start(IndexManagerStartContext context) {
		try {
			/*
			 * Create the lifecycle strategy late to allow the related settings to be changed
			 * after the first phase of bootstrap (useful for compile-time boot).
			 */
			lifecycleStrategy = createLifecycleStrategy( context.getConfigurationPropertySource() );

			lifecycleStrategy.onStart( administrationClient, context );
			serialOrchestrator.start();
			parallelOrchestrator.start();
		}
		catch (RuntimeException e) {
			new SuppressingCloser( e )
					.push( parallelOrchestrator )
					.push( serialOrchestrator );
			throw e;
		}
	}

	@Override
	public void close() {
		try ( Closer<IOException> closer = new Closer<>() ) {
			closer.push( ElasticsearchWorkOrchestratorImplementor::close, serialOrchestrator );
			closer.push( ElasticsearchWorkOrchestratorImplementor::close, parallelOrchestrator );
			closer.push( strategy -> strategy.onStop( administrationClient ), lifecycleStrategy );
		}
		catch (IOException e) {
			throw log.failedToShutdownIndexManager( hibernateSearchIndexName, e, indexingBackendContext.getEventContext() );
		}
	}

	public ElasticsearchIndexModel getModel() {
		return model;
	}

	@Override
	public IndexWorkPlan<ElasticsearchDocumentObjectBuilder> createWorkPlan(SessionContextImplementor sessionContext,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy) {
		// The commit strategy is ignored, because Elasticsearch always commits changes to its transaction log.
		return indexingBackendContext.createWorkPlan(
				serialOrchestrator,
				elasticsearchIndexName,
				refreshStrategy,
				sessionContext
		);
	}

	@Override
	public IndexDocumentWorkExecutor<ElasticsearchDocumentObjectBuilder> createDocumentWorkExecutor(
			SessionContextImplementor sessionContext, DocumentCommitStrategy commitStrategy) {
		// The commit strategy is ignored, because Elasticsearch always commits changes to its transaction log.
		return indexingBackendContext.createDocumentWorkExecutor(
				parallelOrchestrator, elasticsearchIndexName, sessionContext
		);
	}

	@Override
	public IndexWorkExecutor createWorkExecutor(DetachedSessionContextImplementor sessionContext) {
		return indexingBackendContext.createWorkExecutor(
				parallelOrchestrator, elasticsearchIndexName, sessionContext
		);
	}

	@Override
	public IndexScopeBuilder createScopeBuilder(MappingContextImplementor mappingContext) {
		return new ElasticsearchIndexScopeBuilder( searchBackendContext, mappingContext, this );
	}

	@Override
	public void addTo(IndexScopeBuilder builder) {
		if ( !( builder instanceof ElasticsearchIndexScopeBuilder ) ) {
			throw log.cannotMixElasticsearchScopeWithOtherType(
					builder, this, searchBackendContext.getEventContext()
			);
		}

		ElasticsearchIndexScopeBuilder esBuilder = (ElasticsearchIndexScopeBuilder) builder;
		esBuilder.add( searchBackendContext, this );
	}

	@Override
	public String toString() {
		return new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "name=" ).append( hibernateSearchIndexName )
				.append( "elasticsearchName=" ).append( elasticsearchIndexName.original )
				.append( "]" )
				.toString();
	}

	@Override
	public IndexManager toAPI() {
		return this;
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
		return indexingBackendContext.getEventContext().append(
				EventContexts.fromIndexName( hibernateSearchIndexName )
		);
	}

	private ElasticsearchIndexLifecycleStrategy createLifecycleStrategy(ConfigurationPropertySource propertySource) {
		return new ElasticsearchIndexLifecycleStrategy(
				LIFECYCLE_STRATEGY.get( propertySource ),
				new ElasticsearchIndexLifecycleExecutionOptions(
						MANAGEMENT_REQUIRED_STATUS.get( propertySource ),
						MANAGEMENT_REQUIRED_STATUS_WAIT_TIMEOUT.get( propertySource )
				)
		);
	}

}
