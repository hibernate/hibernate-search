/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.impl;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.elasticsearch.ElasticsearchBackend;
import org.hibernate.search.backend.elasticsearch.analysis.model.impl.ElasticsearchAnalysisDefinitionRegistry;
import org.hibernate.search.backend.elasticsearch.document.impl.DocumentMetadataContributor;
import org.hibernate.search.backend.elasticsearch.document.impl.ElasticsearchDocumentObjectBuilder;
import org.hibernate.search.backend.elasticsearch.document.model.dsl.impl.ElasticsearchIndexSchemaRootNodeBuilder;
import org.hibernate.search.backend.elasticsearch.index.naming.impl.IndexNames;
import org.hibernate.search.backend.elasticsearch.index.naming.IndexNamingStrategy;
import org.hibernate.search.backend.elasticsearch.index.impl.ElasticsearchIndexManagerBuilder;
import org.hibernate.search.backend.elasticsearch.index.impl.IndexManagerBackendContext;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.mapping.impl.TypeNameMapping;
import org.hibernate.search.backend.elasticsearch.multitenancy.impl.MultiTenancyStrategy;
import org.hibernate.search.backend.elasticsearch.orchestration.impl.ElasticsearchWorkOrchestratorImplementor;
import org.hibernate.search.backend.elasticsearch.orchestration.impl.ElasticsearchWorkOrchestratorProvider;
import org.hibernate.search.backend.elasticsearch.types.dsl.provider.impl.ElasticsearchIndexFieldTypeFactoryProvider;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.engine.backend.Backend;
import org.hibernate.search.engine.backend.index.spi.IndexManagerBuilder;
import org.hibernate.search.engine.backend.spi.BackendBuildContext;
import org.hibernate.search.engine.backend.spi.BackendImplementor;
import org.hibernate.search.engine.backend.spi.BackendStartContext;
import org.hibernate.search.engine.cfg.spi.ConfigurationPropertySource;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.thread.spi.ThreadPoolProvider;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reporting.EventContext;

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
	private final BeanHolder<? extends IndexNamingStrategy> indexNamingStrategyHolder;
	private final TypeNameMapping typeNameMapping;

	private final ElasticsearchWorkOrchestratorImplementor queryOrchestrator;

	private final EventContext eventContext;

	private final IndexManagerBackendContext indexManagerBackendContext;
	private final IndexNamesRegistry indexNamesRegistry;

	ElasticsearchBackendImpl(String name,
			ElasticsearchLinkImpl link,
			ThreadPoolProvider threadPoolProvider,
			ElasticsearchIndexFieldTypeFactoryProvider typeFactoryProvider,
			Gson userFacingGson,
			ElasticsearchAnalysisDefinitionRegistry analysisDefinitionRegistry,
			MultiTenancyStrategy multiTenancyStrategy,
			BeanHolder<? extends IndexNamingStrategy> indexNamingStrategyHolder,
			TypeNameMapping typeNameMapping,
			FailureHandler failureHandler) {
		this.link = link;
		this.name = name;

		this.orchestratorProvider = new ElasticsearchWorkOrchestratorProvider(
				"Elasticsearch parallel work orchestrator for backend " + name,
				link,
				threadPoolProvider,
				failureHandler
		);
		this.analysisDefinitionRegistry = analysisDefinitionRegistry;
		this.multiTenancyStrategy = multiTenancyStrategy;
		this.typeFactoryProvider = typeFactoryProvider;
		this.indexNamingStrategyHolder = indexNamingStrategyHolder;
		this.typeNameMapping = typeNameMapping;

		this.queryOrchestrator = orchestratorProvider.createParallelOrchestrator( "Elasticsearch query orchestrator for backend " + name );

		this.eventContext = EventContexts.fromBackendName( name );
		this.indexManagerBackendContext = new IndexManagerBackendContext(
				eventContext, link,
				userFacingGson,
				multiTenancyStrategy,
				indexNamingStrategyHolder.get(),
				typeNameMapping,
				orchestratorProvider,
				queryOrchestrator
		);
		this.indexNamesRegistry = new IndexNamesRegistry();
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
	public void start(BackendStartContext context) {
		link.onStart( context.getConfigurationPropertySource() );
		orchestratorProvider.start();
		queryOrchestrator.start();
	}

	@Override
	public CompletableFuture<?> preStop() {
		return CompletableFuture.allOf(
				queryOrchestrator.preStop(),
				orchestratorProvider.preStop()
		);
	}

	@Override
	public void stop() {
		try ( Closer<IOException> closer = new Closer<>() ) {
			closer.push( ElasticsearchWorkOrchestratorImplementor::stop, queryOrchestrator );
			closer.push( ElasticsearchWorkOrchestratorProvider::stop, orchestratorProvider );
			// Close the client after the orchestrators, when we're sure all works have been performed
			closer.push( ElasticsearchLinkImpl::onStop, link );
			closer.push( BeanHolder::close, indexNamingStrategyHolder );
		}
		catch (IOException | RuntimeException e) {
			throw log.failedToShutdownBackend( e, eventContext );
		}
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
			String hibernateSearchIndexName,
			String mappedTypeName, boolean multiTenancyEnabled, BackendBuildContext buildContext,
			ConfigurationPropertySource propertySource) {
		if ( multiTenancyEnabled && !multiTenancyStrategy.isMultiTenancySupported() ) {
			throw log.multiTenancyRequiredButNotSupportedByBackend( hibernateSearchIndexName, eventContext );
		}

		EventContext indexEventContext = EventContexts.fromIndexName( hibernateSearchIndexName );

		IndexNames indexNames = createIndexNames( indexEventContext, hibernateSearchIndexName, mappedTypeName );

		return new ElasticsearchIndexManagerBuilder(
				indexManagerBackendContext,
				createIndexSchemaRootNodeBuilder( indexEventContext, indexNames, mappedTypeName ),
				createDocumentMetadataContributors( mappedTypeName )
		);
	}

	private IndexNames createIndexNames(EventContext indexEventContext, String hibernateSearchIndexName, String mappedTypeName) {
		IndexNamingStrategy indexNamingStrategy = indexNamingStrategyHolder.get();
		URLEncodedString writeAlias = IndexNames.encodeName( indexNamingStrategy.createWriteAlias( hibernateSearchIndexName ) );
		URLEncodedString readAlias = IndexNames.encodeName( indexNamingStrategy.createReadAlias( hibernateSearchIndexName ) );

		if ( writeAlias.equals( readAlias ) ) {
			throw log.sameWriteAndReadAliases( writeAlias, indexEventContext );
		}

		IndexNames indexNames = new IndexNames(
				hibernateSearchIndexName,
				writeAlias,
				readAlias
		);

		// This will check that names are unique.
		indexNamesRegistry.register( indexNames );

		// This will allow the type mapping to resolve the type name from the index name.
		typeNameMapping.register( indexNames, mappedTypeName );

		return indexNames;
	}

	private ElasticsearchIndexSchemaRootNodeBuilder createIndexSchemaRootNodeBuilder(EventContext indexEventContext,
			IndexNames indexNames, String mappedTypeName) {
		ElasticsearchIndexSchemaRootNodeBuilder builder = new ElasticsearchIndexSchemaRootNodeBuilder(
				typeFactoryProvider,
				indexEventContext,
				indexNames,
				mappedTypeName,
				analysisDefinitionRegistry
		);

		typeNameMapping.getIndexSchemaRootContributor()
				.ifPresent( builder::addSchemaRootContributor );

		multiTenancyStrategy.getIndexSchemaRootContributor()
				.ifPresent( builder::addSchemaRootContributor );

		return builder;
	}

	private List<DocumentMetadataContributor> createDocumentMetadataContributors(String mappedTypeName) {
		List<DocumentMetadataContributor> contributors = new ArrayList<>();
		typeNameMapping.getDocumentMetadataContributor( mappedTypeName )
				.ifPresent( contributors::add );
		multiTenancyStrategy.getDocumentMetadataContributor()
				.ifPresent( contributors::add );
		return contributors;
	}
}
