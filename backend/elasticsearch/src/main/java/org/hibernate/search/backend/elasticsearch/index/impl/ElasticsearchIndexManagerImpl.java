/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.index.impl;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.elasticsearch.index.IndexLifecycleStrategyName;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexSettings;
import org.hibernate.search.backend.elasticsearch.index.IndexStatus;
import org.hibernate.search.backend.elasticsearch.document.impl.ElasticsearchDocumentObjectBuilder;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexModel;
import org.hibernate.search.backend.elasticsearch.index.ElasticsearchIndexManager;
import org.hibernate.search.backend.elasticsearch.index.admin.impl.ElasticsearchIndexAdministrationClient;
import org.hibernate.search.backend.elasticsearch.index.admin.impl.ElasticsearchIndexLifecycleExecutionOptions;
import org.hibernate.search.backend.elasticsearch.index.management.impl.ElasticsearchIndexLifecycleStrategy;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.orchestration.impl.ElasticsearchWorkOrchestratorImplementor;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.index.IndexManager;
import org.hibernate.search.engine.backend.index.spi.IndexManagerStartContext;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.backend.work.execution.spi.IndexWorkExecutor;
import org.hibernate.search.engine.backend.index.spi.IndexManagerImplementor;
import org.hibernate.search.engine.backend.scope.spi.IndexScopeBuilder;
import org.hibernate.search.engine.backend.work.execution.spi.IndexDocumentWorkExecutor;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlan;
import org.hibernate.search.engine.cfg.spi.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.backend.session.spi.DetachedBackendSessionContext;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.backend.mapping.spi.BackendMappingContext;
import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;
import org.hibernate.search.util.common.impl.SuppressingCloser;
import org.hibernate.search.util.common.reporting.EventContext;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;



class ElasticsearchIndexManagerImpl implements IndexManagerImplementor<ElasticsearchDocumentObjectBuilder>,
		ElasticsearchIndexManager {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final ConfigurationProperty<IndexLifecycleStrategyName> LIFECYCLE_STRATEGY =
			ConfigurationProperty.forKey( ElasticsearchIndexSettings.LIFECYCLE_STRATEGY )
					.as( IndexLifecycleStrategyName.class, IndexLifecycleStrategyName::of )
					.withDefault( ElasticsearchIndexSettings.Defaults.LIFECYCLE_STRATEGY )
					.build();

	private static final ConfigurationProperty<IndexStatus> LIFECYCLE_MINIMAL_REQUIRED_STATUS =
			ConfigurationProperty.forKey( ElasticsearchIndexSettings.LIFECYCLE_MINIMAL_REQUIRED_STATUS )
					.as( IndexStatus.class, IndexStatus::of )
					.withDefault( ElasticsearchIndexSettings.Defaults.LIFECYCLE_MINIMAL_REQUIRED_STATUS )
					.build();

	private static final ConfigurationProperty<Integer> LIFECYCLE_MINIMAL_REQUIRED_STATUS_WAIT_TIMEOUT =
			ConfigurationProperty.forKey( ElasticsearchIndexSettings.LIFECYCLE_MINIMAL_REQUIRED_STATUS_WAIT_TIMEOUT )
					.asInteger()
					.withDefault( ElasticsearchIndexSettings.Defaults.LIFECYCLE_MINIMAL_REQUIRED_STATUS_WAIT_TIMEOUT )
					.build();

	private final IndexManagerBackendContext backendContext;

	private final String hibernateSearchIndexName;
	private final URLEncodedString elasticsearchIndexName;
	private final ElasticsearchIndexModel model;

	private final ElasticsearchWorkOrchestratorImplementor serialOrchestrator;
	private final ElasticsearchWorkOrchestratorImplementor parallelOrchestrator;

	private final ElasticsearchIndexAdministrationClient administrationClient;

	private ElasticsearchIndexLifecycleStrategy lifecycleStrategy;

	ElasticsearchIndexManagerImpl(IndexManagerBackendContext backendContext,
			String hibernateSearchIndexName, URLEncodedString elasticsearchIndexName,
			ElasticsearchIndexModel model) {
		this.backendContext = backendContext;
		this.hibernateSearchIndexName = hibernateSearchIndexName;
		this.elasticsearchIndexName = elasticsearchIndexName;
		this.model = model;
		this.parallelOrchestrator = backendContext.createParallelOrchestrator( elasticsearchIndexName.original );
		this.serialOrchestrator = backendContext.createSerialOrchestrator( elasticsearchIndexName.original );
		this.administrationClient = backendContext.createAdministrationClient(
				elasticsearchIndexName, model
		);
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
			throw log.failedToShutdownIndexManager( hibernateSearchIndexName, e, backendContext.getEventContext() );
		}
	}

	public ElasticsearchIndexModel getModel() {
		return model;
	}

	@Override
	public IndexIndexingPlan<ElasticsearchDocumentObjectBuilder> createIndexingPlan(BackendSessionContext sessionContext,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy) {
		// The commit strategy is ignored, because Elasticsearch always commits changes to its transaction log.
		return backendContext.createIndexingPlan(
				serialOrchestrator,
				elasticsearchIndexName,
				refreshStrategy,
				sessionContext
		);
	}

	@Override
	public IndexDocumentWorkExecutor<ElasticsearchDocumentObjectBuilder> createDocumentWorkExecutor(
			BackendSessionContext sessionContext, DocumentCommitStrategy commitStrategy) {
		// The commit strategy is ignored, because Elasticsearch always commits changes to its transaction log.
		return backendContext.createDocumentWorkExecutor(
				parallelOrchestrator, elasticsearchIndexName, sessionContext
		);
	}

	@Override
	public IndexWorkExecutor createWorkExecutor(DetachedBackendSessionContext sessionContext) {
		return backendContext.createWorkExecutor(
				parallelOrchestrator, elasticsearchIndexName, sessionContext
		);
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
				EventContexts.fromIndexName( hibernateSearchIndexName )
		);
	}

	private ElasticsearchIndexLifecycleStrategy createLifecycleStrategy(ConfigurationPropertySource propertySource) {
		return new ElasticsearchIndexLifecycleStrategy(
				LIFECYCLE_STRATEGY.get( propertySource ),
				new ElasticsearchIndexLifecycleExecutionOptions(
						LIFECYCLE_MINIMAL_REQUIRED_STATUS.get( propertySource ),
						LIFECYCLE_MINIMAL_REQUIRED_STATUS_WAIT_TIMEOUT.get( propertySource )
				)
		);
	}

}
