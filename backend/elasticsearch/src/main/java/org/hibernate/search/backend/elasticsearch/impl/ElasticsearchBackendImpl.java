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
import org.hibernate.search.backend.elasticsearch.document.model.dsl.impl.ElasticsearchIndexSchemaRootNodeBuilder;
import org.hibernate.search.backend.elasticsearch.index.layout.impl.IndexNames;
import org.hibernate.search.backend.elasticsearch.index.layout.IndexLayoutStrategy;
import org.hibernate.search.backend.elasticsearch.index.impl.ElasticsearchIndexManagerBuilder;
import org.hibernate.search.backend.elasticsearch.index.impl.IndexManagerBackendContext;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.mapping.impl.TypeNameMapping;
import org.hibernate.search.backend.elasticsearch.multitenancy.impl.MultiTenancyStrategy;
import org.hibernate.search.backend.elasticsearch.orchestration.impl.ElasticsearchSimpleWorkOrchestrator;
import org.hibernate.search.backend.elasticsearch.resources.impl.BackendThreads;
import org.hibernate.search.backend.elasticsearch.types.dsl.provider.impl.ElasticsearchIndexFieldTypeFactoryProvider;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.engine.backend.Backend;
import org.hibernate.search.engine.backend.index.spi.IndexManagerBuilder;
import org.hibernate.search.engine.backend.spi.BackendBuildContext;
import org.hibernate.search.engine.backend.spi.BackendImplementor;
import org.hibernate.search.engine.backend.spi.BackendStartContext;
import org.hibernate.search.engine.cfg.spi.ConfigurationPropertySource;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reporting.EventContext;

import com.google.gson.Gson;



class ElasticsearchBackendImpl implements BackendImplementor,
		ElasticsearchBackend {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final String name;

	private final BackendThreads threads;
	private final ElasticsearchLinkImpl link;

	private final ElasticsearchSimpleWorkOrchestrator generalPurposeOrchestrator;

	private final ElasticsearchIndexFieldTypeFactoryProvider typeFactoryProvider;
	private final ElasticsearchAnalysisDefinitionRegistry analysisDefinitionRegistry;
	private final MultiTenancyStrategy multiTenancyStrategy;
	private final BeanHolder<? extends IndexLayoutStrategy> indexLayoutStrategyHolder;
	private final TypeNameMapping typeNameMapping;

	private final EventContext eventContext;

	private final IndexManagerBackendContext indexManagerBackendContext;
	private final IndexNamesRegistry indexNamesRegistry;

	ElasticsearchBackendImpl(String name,
			BackendThreads threads,
			ElasticsearchLinkImpl link,
			ElasticsearchIndexFieldTypeFactoryProvider typeFactoryProvider,
			Gson userFacingGson,
			ElasticsearchAnalysisDefinitionRegistry analysisDefinitionRegistry,
			MultiTenancyStrategy multiTenancyStrategy,
			BeanHolder<? extends IndexLayoutStrategy> indexLayoutStrategyHolder,
			TypeNameMapping typeNameMapping,
			FailureHandler failureHandler) {
		this.name = name;
		this.threads = threads;
		this.link = link;

		this.generalPurposeOrchestrator = new ElasticsearchSimpleWorkOrchestrator(
				"Elasticsearch general purpose orchestrator for backend " + name,
				link
		);
		this.analysisDefinitionRegistry = analysisDefinitionRegistry;
		this.multiTenancyStrategy = multiTenancyStrategy;
		this.typeFactoryProvider = typeFactoryProvider;
		this.indexLayoutStrategyHolder = indexLayoutStrategyHolder;
		this.typeNameMapping = typeNameMapping;

		this.eventContext = EventContexts.fromBackendName( name );
		this.indexManagerBackendContext = new IndexManagerBackendContext(
				this, eventContext, threads, link,
				userFacingGson,
				multiTenancyStrategy,
				indexLayoutStrategyHolder.get(),
				typeNameMapping,
				failureHandler,
				generalPurposeOrchestrator
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
		threads.onStart( context.configurationPropertySource(), context.threadPoolProvider() );
		link.onStart( context.configurationPropertySource() );
		generalPurposeOrchestrator.start( context.configurationPropertySource() );
	}

	@Override
	public CompletableFuture<?> preStop() {
		return generalPurposeOrchestrator.preStop();
	}

	@Override
	public void stop() {
		try ( Closer<IOException> closer = new Closer<>() ) {
			closer.push( ElasticsearchSimpleWorkOrchestrator::stop, generalPurposeOrchestrator );
			closer.push( ElasticsearchLinkImpl::onStop, link );
			closer.push( BeanHolder::close, indexLayoutStrategyHolder );
			closer.push( BackendThreads::onStop, threads );
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
	public <T> T client(Class<T> clientClass) {
		return link.getClient().unwrap( clientClass );
	}

	@Override
	public IndexManagerBuilder createIndexManagerBuilder(
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
		IndexLayoutStrategy indexLayoutStrategy = indexLayoutStrategyHolder.get();
		URLEncodedString writeAlias = IndexNames.encodeName( indexLayoutStrategy.createWriteAlias( hibernateSearchIndexName ) );
		URLEncodedString readAlias = IndexNames.encodeName( indexLayoutStrategy.createReadAlias( hibernateSearchIndexName ) );

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
