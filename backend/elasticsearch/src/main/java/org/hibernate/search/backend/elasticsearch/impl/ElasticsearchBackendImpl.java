/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.impl;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.search.backend.elasticsearch.analysis.model.impl.ElasticsearchAnalysisDefinitionRegistry;
import org.hibernate.search.backend.elasticsearch.index.settings.impl.ElasticsearchIndexSettingsBuilder;
import org.hibernate.search.backend.elasticsearch.orchestration.impl.ElasticsearchWorkOrchestratorImplementor;
import org.hibernate.search.backend.elasticsearch.orchestration.impl.ElasticsearchWorkOrchestratorProvider;
import org.hibernate.search.backend.elasticsearch.types.dsl.provider.impl.ElasticsearchIndexFieldTypeFactoryProvider;
import org.hibernate.search.engine.backend.Backend;
import org.hibernate.search.backend.elasticsearch.ElasticsearchBackend;
import org.hibernate.search.backend.elasticsearch.document.impl.ElasticsearchDocumentObjectBuilder;
import org.hibernate.search.backend.elasticsearch.document.model.dsl.impl.ElasticsearchIndexSchemaRootNodeBuilder;
import org.hibernate.search.backend.elasticsearch.index.impl.ElasticsearchIndexManagerBuilder;
import org.hibernate.search.backend.elasticsearch.index.impl.IndexManagerBackendContext;
import org.hibernate.search.backend.elasticsearch.multitenancy.impl.MultiTenancyStrategy;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.engine.backend.index.spi.IndexManagerBuilder;
import org.hibernate.search.engine.backend.spi.BackendImplementor;
import org.hibernate.search.engine.backend.spi.BackendStartContext;
import org.hibernate.search.engine.cfg.spi.ConfigurationPropertySource;
import org.hibernate.search.engine.backend.spi.BackendBuildContext;
import org.hibernate.search.engine.common.spi.LogErrorHandler;
import org.hibernate.search.util.common.reporting.EventContext;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import com.google.gson.Gson;



class ElasticsearchBackendImpl implements BackendImplementor<ElasticsearchDocumentObjectBuilder>,
		ElasticsearchBackend {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final ElasticsearchLinkImpl link;

	private final String name;
	private final ElasticsearchWorkOrchestratorProvider orchestratorProvider;

	private final ElasticsearchIndexFieldTypeFactoryProvider typeFactoryProvider;

	private final ElasticsearchAnalysisDefinitionRegistry analysisDefinitionRegistry;

	private final MultiTenancyStrategy multiTenancyStrategy;

	private final ElasticsearchWorkOrchestratorImplementor queryOrchestrator;

	private final Map<String, String> hibernateSearchIndexNamesByElasticsearchIndexNames = new ConcurrentHashMap<>();

	private final EventContext eventContext;
	private final IndexManagerBackendContext indexManagerBackendContext;

	ElasticsearchBackendImpl(ElasticsearchLinkImpl link,
			String name,
			ElasticsearchIndexFieldTypeFactoryProvider typeFactoryProvider,
			Gson userFacingGson,
			ElasticsearchAnalysisDefinitionRegistry analysisDefinitionRegistry,
			MultiTenancyStrategy multiTenancyStrategy) {
		this.link = link;
		this.name = name;

		this.orchestratorProvider = new ElasticsearchWorkOrchestratorProvider(
				"Elasticsearch parallel work orchestrator for backend " + name,
				link,
				// TODO the LogErrorHandler should be replaced with a user-configurable instance at some point. See HSEARCH-3110.
				new LogErrorHandler()
		);
		this.analysisDefinitionRegistry = analysisDefinitionRegistry;
		this.multiTenancyStrategy = multiTenancyStrategy;
		this.queryOrchestrator = orchestratorProvider.createParallelOrchestrator( "Elasticsearch query orchestrator for backend " + name );

		this.typeFactoryProvider = typeFactoryProvider;

		this.eventContext = EventContexts.fromBackendName( name );
		this.indexManagerBackendContext = new IndexManagerBackendContext(
				eventContext, link,
				userFacingGson,
				( String elasticsearchIndexName ) -> {
					String result = hibernateSearchIndexNamesByElasticsearchIndexNames.get( elasticsearchIndexName );
					if ( result == null ) {
						throw log.elasticsearchResponseUnknownIndexName( elasticsearchIndexName, eventContext );
					}
					return result;
				},
				multiTenancyStrategy,
				orchestratorProvider,
				queryOrchestrator
		);
	}

	@Override
	public String toString() {
		return new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "name=" ).append( name )
				.append( "]" )
				.toString();
	}

	@Override
	public void close() {
		try ( Closer<IOException> closer = new Closer<>() ) {
			closer.push( ElasticsearchWorkOrchestratorImplementor::close, queryOrchestrator );
			closer.push( ElasticsearchWorkOrchestratorProvider::close, orchestratorProvider );
			// Close the client after the orchestrators, when we're sure all works have been performed
			closer.push( ElasticsearchLinkImpl::onStop, link );
		}
		catch (IOException | RuntimeException e) {
			throw log.failedToShutdownBackend( e, eventContext );
		}
	}

	@Override
	public void start(BackendStartContext context) {
		link.onStart( context.getConfigurationPropertySource() );
		orchestratorProvider.start();
		queryOrchestrator.start();
	}

	@Override
	@SuppressWarnings("unchecked") // Checked using reflection
	public <T> T unwrap(Class<T> clazz) {
		if ( clazz.isAssignableFrom( ElasticsearchBackend.class ) ) {
			return (T) this;
		}
		throw log.backendUnwrappingWithUnknownType( clazz, ElasticsearchBackend.class, eventContext );
	}

	@Override
	public Backend toAPI() {
		return this;
	}

	@Override
	public <T> T getClient(Class<T> clientClass) {
		return link.getClient().unwrap( clientClass );
	}

	@Override
	public IndexManagerBuilder<ElasticsearchDocumentObjectBuilder> createIndexManagerBuilder(
			String hibernateSearchIndexName, boolean multiTenancyEnabled, BackendBuildContext buildContext, ConfigurationPropertySource propertySource) {
		if ( multiTenancyEnabled && !multiTenancyStrategy.isMultiTenancySupported() ) {
			throw log.multiTenancyRequiredButNotSupportedByBackend( hibernateSearchIndexName, eventContext );
		}

		String elasticsearchIndexName = ElasticsearchIndexNameNormalizer.normalize( hibernateSearchIndexName );
		String existingHibernateSearchIndexName = hibernateSearchIndexNamesByElasticsearchIndexNames.putIfAbsent(
				elasticsearchIndexName, hibernateSearchIndexName
		);
		if ( existingHibernateSearchIndexName != null ) {
			throw log.duplicateNormalizedIndexNames(
					existingHibernateSearchIndexName, hibernateSearchIndexName, elasticsearchIndexName,
					eventContext
			);
		}

		EventContext indexEventContext = EventContexts.fromIndexName( hibernateSearchIndexName );

		ElasticsearchIndexSchemaRootNodeBuilder indexSchemaRootNodeBuilder =
				new ElasticsearchIndexSchemaRootNodeBuilder(
						typeFactoryProvider,
						indexEventContext,
						multiTenancyStrategy
				);

		ElasticsearchIndexSettingsBuilder settingsBuilder =
				new ElasticsearchIndexSettingsBuilder( analysisDefinitionRegistry );

		return new ElasticsearchIndexManagerBuilder(
				indexManagerBackendContext,
				hibernateSearchIndexName, elasticsearchIndexName,
				indexSchemaRootNodeBuilder, settingsBuilder
		);
	}
}
