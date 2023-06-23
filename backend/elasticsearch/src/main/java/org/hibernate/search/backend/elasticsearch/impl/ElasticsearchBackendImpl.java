/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.elasticsearch.ElasticsearchBackend;
import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurer;
import org.hibernate.search.backend.elasticsearch.analysis.model.dsl.impl.ElasticsearchAnalysisConfigurationContextImpl;
import org.hibernate.search.backend.elasticsearch.analysis.model.impl.ElasticsearchAnalysisDefinitionRegistry;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexSettings;
import org.hibernate.search.backend.elasticsearch.document.impl.DocumentMetadataContributor;
import org.hibernate.search.backend.elasticsearch.document.model.dsl.impl.ElasticsearchIndexRootBuilder;
import org.hibernate.search.backend.elasticsearch.index.DynamicMapping;
import org.hibernate.search.backend.elasticsearch.index.impl.ElasticsearchIndexManagerBuilder;
import org.hibernate.search.backend.elasticsearch.index.impl.IndexManagerBackendContext;
import org.hibernate.search.backend.elasticsearch.index.layout.IndexLayoutStrategy;
import org.hibernate.search.backend.elasticsearch.index.layout.impl.IndexNames;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.RootTypeMapping;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.settings.impl.IndexSettings;
import org.hibernate.search.backend.elasticsearch.mapping.impl.TypeNameMapping;
import org.hibernate.search.backend.elasticsearch.multitenancy.impl.MultiTenancyStrategy;
import org.hibernate.search.backend.elasticsearch.orchestration.impl.ElasticsearchSimpleWorkOrchestrator;
import org.hibernate.search.backend.elasticsearch.resources.impl.BackendThreads;
import org.hibernate.search.backend.elasticsearch.types.dsl.provider.impl.ElasticsearchIndexFieldTypeFactoryProvider;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.engine.backend.Backend;
import org.hibernate.search.engine.backend.index.spi.IndexManagerBuilder;
import org.hibernate.search.engine.backend.mapping.spi.BackendMapperContext;
import org.hibernate.search.engine.backend.spi.BackendBuildContext;
import org.hibernate.search.engine.backend.spi.BackendImplementor;
import org.hibernate.search.engine.backend.spi.BackendStartContext;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.cfg.spi.OptionalConfigurationProperty;
import org.hibernate.search.engine.common.timing.spi.TimingSource;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reporting.EventContext;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

class ElasticsearchBackendImpl
		implements BackendImplementor,
		ElasticsearchBackend {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final OptionalConfigurationProperty<
			List<BeanReference<? extends ElasticsearchAnalysisConfigurer>>> ANALYSIS_CONFIGURER =
					ConfigurationProperty.forKey( ElasticsearchIndexSettings.ANALYSIS_CONFIGURER )
							.asBeanReference( ElasticsearchAnalysisConfigurer.class )
							.multivalued()
							.build();

	private static final ConfigurationProperty<DynamicMapping> DYNAMIC_MAPPING =
			ConfigurationProperty.forKey( ElasticsearchIndexSettings.DYNAMIC_MAPPING )
					.as( DynamicMapping.class, DynamicMapping::of )
					.withDefault( ElasticsearchIndexSettings.Defaults.DYNAMIC_MAPPING )
					.build();

	private static final OptionalConfigurationProperty<String> SCHEMA_MANAGEMENT_SETTINGS_FILE =
			ConfigurationProperty.forKey( ElasticsearchIndexSettings.SCHEMA_MANAGEMENT_SETTINGS_FILE )
					.asString()
					.build();

	private static final OptionalConfigurationProperty<String> SCHEMA_MANAGEMENT_MAPPING_FILE =
			ConfigurationProperty.forKey( ElasticsearchIndexSettings.SCHEMA_MANAGEMENT_MAPPING_FILE )
					.asString()
					.build();

	private final Optional<String> backendName;
	private final EventContext eventContext;

	private final BackendThreads threads;
	private final ElasticsearchLinkImpl link;

	private final ElasticsearchSimpleWorkOrchestrator generalPurposeOrchestrator;

	private final ElasticsearchIndexFieldTypeFactoryProvider typeFactoryProvider;
	private final Gson userFacingGson;
	private final MultiTenancyStrategy multiTenancyStrategy;
	private final BeanHolder<? extends IndexLayoutStrategy> indexLayoutStrategyHolder;
	private final TypeNameMapping typeNameMapping;

	private final IndexManagerBackendContext indexManagerBackendContext;
	private final IndexNamesRegistry indexNamesRegistry;

	ElasticsearchBackendImpl(Optional<String> backendName,
			EventContext eventContext,
			BackendThreads threads,
			ElasticsearchLinkImpl link,
			ElasticsearchIndexFieldTypeFactoryProvider typeFactoryProvider,
			Gson userFacingGson,
			MultiTenancyStrategy multiTenancyStrategy,
			BeanHolder<? extends IndexLayoutStrategy> indexLayoutStrategyHolder,
			TypeNameMapping typeNameMapping,
			FailureHandler failureHandler, TimingSource timingSource) {
		this.backendName = backendName;
		this.eventContext = eventContext;
		this.threads = threads;
		this.link = link;

		this.generalPurposeOrchestrator = new ElasticsearchSimpleWorkOrchestrator(
				"Elasticsearch general purpose orchestrator - " + eventContext.render(),
				link
		);
		this.multiTenancyStrategy = multiTenancyStrategy;
		this.typeFactoryProvider = typeFactoryProvider;
		this.userFacingGson = userFacingGson;
		this.indexLayoutStrategyHolder = indexLayoutStrategyHolder;
		this.typeNameMapping = typeNameMapping;

		this.indexManagerBackendContext = new IndexManagerBackendContext(
				this, eventContext, threads, link,
				userFacingGson,
				multiTenancyStrategy,
				indexLayoutStrategyHolder.get(),
				typeNameMapping,
				failureHandler, timingSource,
				generalPurposeOrchestrator
		);
		this.indexNamesRegistry = new IndexNamesRegistry();
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + eventContext.render() + "]";
	}

	@Override
	public void start(BackendStartContext context) {
		threads.onStart( context.configurationPropertySource(), context.beanResolver(), context.threadPoolProvider() );
		link.onStart( context.beanResolver(), context.configurationPropertySource() );
		generalPurposeOrchestrator.start( context.configurationPropertySource() );
	}

	@Override
	public CompletableFuture<?> preStop() {
		return generalPurposeOrchestrator.preStop();
	}

	@Override
	public void stop() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.push( ElasticsearchSimpleWorkOrchestrator::stop, generalPurposeOrchestrator );
			closer.push( ElasticsearchLinkImpl::onStop, link );
			closer.push( BeanHolder::close, indexLayoutStrategyHolder );
			closer.push( BackendThreads::onStop, threads );
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
	public Optional<String> name() {
		return backendName;
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
			String hibernateSearchIndexName, String mappedTypeName, BackendBuildContext buildContext,
			BackendMapperContext backendMapperContext,
			ConfigurationPropertySource propertySource) {

		EventContext indexEventContext = EventContexts.fromIndexName( hibernateSearchIndexName );

		IndexNames indexNames = createIndexNames( indexEventContext, hibernateSearchIndexName, mappedTypeName );

		ElasticsearchAnalysisDefinitionRegistry analysisDefinitionRegistry =
				createAnalysisDefinitionRegistry( buildContext, indexEventContext, propertySource );

		return new ElasticsearchIndexManagerBuilder(
				indexManagerBackendContext,
				createIndexSchemaRootNodeBuilder( indexEventContext, backendMapperContext, indexNames, mappedTypeName,
						analysisDefinitionRegistry,
						customIndexSettings( buildContext, propertySource, indexEventContext ),
						customIndexMappings( buildContext, propertySource, indexEventContext ),
						DYNAMIC_MAPPING.get( propertySource )
				),
				createDocumentMetadataContributors( mappedTypeName )
		);
	}

	private IndexNames createIndexNames(EventContext indexEventContext, String hibernateSearchIndexName,
			String mappedTypeName) {
		IndexLayoutStrategy indexLayoutStrategy = indexLayoutStrategyHolder.get();
		URLEncodedString writeAlias = IndexNames.encodeName( indexLayoutStrategy.createWriteAlias( hibernateSearchIndexName ) );
		URLEncodedString readAlias = IndexNames.encodeName( indexLayoutStrategy.createReadAlias( hibernateSearchIndexName ) );

		URLEncodedString primaryName = null;
		if ( writeAlias == null || readAlias == null ) {
			primaryName = IndexNames.encodeName(
					indexLayoutStrategy.createInitialElasticsearchIndexName( hibernateSearchIndexName ) );
		}
		else if ( writeAlias.equals( readAlias ) ) {
			throw log.sameWriteAndReadAliases( writeAlias, indexEventContext );
		}

		URLEncodedString readName = readAlias != null ? readAlias : primaryName;
		URLEncodedString writeName = writeAlias != null ? writeAlias : primaryName;

		IndexNames indexNames = new IndexNames( hibernateSearchIndexName,
				writeName, writeAlias != null, readName, readAlias != null );

		// This will check that names are unique.
		indexNamesRegistry.register( indexNames );

		// This will allow the type mapping to resolve the type name from the index name.
		typeNameMapping.register( indexNames, mappedTypeName );

		return indexNames;
	}

	private ElasticsearchIndexRootBuilder createIndexSchemaRootNodeBuilder(EventContext indexEventContext,
			BackendMapperContext backendMapperContext, IndexNames indexNames, String mappedTypeName,
			ElasticsearchAnalysisDefinitionRegistry analysisDefinitionRegistry,
			IndexSettings customIndexSettings, RootTypeMapping customIndexMapping,
			DynamicMapping dynamicMapping) {

		ElasticsearchIndexRootBuilder builder = new ElasticsearchIndexRootBuilder(
				typeFactoryProvider,
				indexEventContext,
				backendMapperContext,
				indexNames,
				mappedTypeName,
				analysisDefinitionRegistry,
				customIndexSettings, customIndexMapping,
				dynamicMapping
		);

		typeNameMapping.getIndexSchemaRootContributor()
				.ifPresent( builder::addSchemaRootContributor );
		typeNameMapping.getImplicitFieldContributor()
				.ifPresent( builder::addImplicitFieldContributor );

		multiTenancyStrategy.indexSchemaRootContributor()
				.ifPresent( builder::addSchemaRootContributor );

		return builder;
	}

	private ElasticsearchAnalysisDefinitionRegistry createAnalysisDefinitionRegistry(BackendBuildContext buildContext,
			EventContext indexEventContext, ConfigurationPropertySource propertySource) {
		try {
			BeanResolver beanResolver = buildContext.beanResolver();
			// Apply the user-provided analysis configurers if necessary
			return ANALYSIS_CONFIGURER.getAndMap( propertySource, beanResolver::resolve )
					.map( holder -> {
						try ( BeanHolder<List<ElasticsearchAnalysisConfigurer>> configurerHolder = holder ) {
							ElasticsearchAnalysisConfigurationContextImpl collector =
									new ElasticsearchAnalysisConfigurationContextImpl();
							for ( ElasticsearchAnalysisConfigurer configurer : configurerHolder.get() ) {
								configurer.configure( collector );
							}
							return new ElasticsearchAnalysisDefinitionRegistry( collector );
						}
					} )
					// Otherwise just use an empty registry
					.orElseGet( ElasticsearchAnalysisDefinitionRegistry::new );
		}
		catch (Exception e) {
			throw log.unableToApplyAnalysisConfiguration( e.getMessage(), e, indexEventContext );
		}
	}

	private List<DocumentMetadataContributor> createDocumentMetadataContributors(String mappedTypeName) {
		List<DocumentMetadataContributor> contributors = new ArrayList<>();
		typeNameMapping.getDocumentMetadataContributor( mappedTypeName )
				.ifPresent( contributors::add );
		multiTenancyStrategy.documentMetadataContributor()
				.ifPresent( contributors::add );
		return contributors;
	}

	private IndexSettings customIndexSettings(BackendBuildContext buildContext,
			ConfigurationPropertySource propertySource, EventContext indexEventContext) {

		Optional<String> schemaManagementSettingsFile = SCHEMA_MANAGEMENT_SETTINGS_FILE.get( propertySource );
		if ( !schemaManagementSettingsFile.isPresent() ) {
			return null;
		}

		String filePath = schemaManagementSettingsFile.get();
		try ( InputStream inputStream = buildContext.resourceResolver().locateResourceStream( filePath ) ) {
			if ( inputStream == null ) {
				throw log.customIndexSettingsFileNotFound( filePath, indexEventContext );
			}
			try ( Reader reader = new InputStreamReader( inputStream, StandardCharsets.UTF_8 ) ) {
				return userFacingGson.fromJson( reader, IndexSettings.class );
			}
		}
		catch (IOException e) {
			throw log.customIndexSettingsErrorOnLoading( filePath, e.getMessage(), e, indexEventContext );
		}
		catch (JsonSyntaxException e) {
			throw log.customIndexSettingsJsonSyntaxErrors( filePath, e.getMessage(), e, indexEventContext );
		}
	}

	private RootTypeMapping customIndexMappings(BackendBuildContext buildContext,
			ConfigurationPropertySource propertySource, EventContext indexEventContext) {

		Optional<String> schemaManagementMappingsFile = SCHEMA_MANAGEMENT_MAPPING_FILE.get( propertySource );
		if ( !schemaManagementMappingsFile.isPresent() ) {
			return null;
		}

		String filePath = schemaManagementMappingsFile.get();
		try ( InputStream inputStream = buildContext.resourceResolver().locateResourceStream( filePath ) ) {
			if ( inputStream == null ) {
				throw log.customIndexMappingFileNotFound( filePath, indexEventContext );
			}
			try ( Reader reader = new InputStreamReader( inputStream, StandardCharsets.UTF_8 ) ) {
				return userFacingGson.fromJson( reader, RootTypeMapping.class );
			}
		}
		catch (IOException e) {
			throw log.customIndexMappingErrorOnLoading( filePath, e.getMessage(), e, indexEventContext );
		}
		catch (JsonSyntaxException e) {
			throw log.customIndexMappingJsonSyntaxErrors( filePath, e.getMessage(), e, indexEventContext );
		}
	}
}
