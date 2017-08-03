/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.similarities.Similarity;
import org.hibernate.search.backend.BackendFactory;
import org.hibernate.search.backend.FlushLuceneWork;
import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.OptimizeLuceneWork;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.elasticsearch.cfg.ElasticsearchEnvironment;
import org.hibernate.search.elasticsearch.cfg.ElasticsearchIndexStatus;
import org.hibernate.search.elasticsearch.cfg.IndexSchemaManagementStrategy;
import org.hibernate.search.elasticsearch.client.impl.URLEncodedString;
import org.hibernate.search.elasticsearch.logging.impl.Log;
import org.hibernate.search.elasticsearch.processor.impl.BarrierElasticsearchWorkOrchestrator;
import org.hibernate.search.elasticsearch.processor.impl.ElasticsearchWorkProcessor;
import org.hibernate.search.elasticsearch.schema.impl.ElasticsearchSchemaCreator;
import org.hibernate.search.elasticsearch.schema.impl.ElasticsearchSchemaDropper;
import org.hibernate.search.elasticsearch.schema.impl.ElasticsearchSchemaMigrator;
import org.hibernate.search.elasticsearch.schema.impl.ElasticsearchSchemaTranslator;
import org.hibernate.search.elasticsearch.schema.impl.ElasticsearchSchemaValidator;
import org.hibernate.search.elasticsearch.schema.impl.ExecutionOptions;
import org.hibernate.search.elasticsearch.schema.impl.model.DynamicType;
import org.hibernate.search.elasticsearch.schema.impl.model.IndexMetadata;
import org.hibernate.search.elasticsearch.spi.ElasticsearchIndexManagerType;
import org.hibernate.search.elasticsearch.work.impl.ElasticsearchWork;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.indexes.serialization.spi.LuceneWorkSerializer;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.indexes.spi.IndexManagerType;
import org.hibernate.search.indexes.spi.IndexNameNormalizer;
import org.hibernate.search.indexes.spi.ReaderProvider;
import org.hibernate.search.spi.IndexedTypeIdentifier;
import org.hibernate.search.spi.IndexedTypeSet;
import org.hibernate.search.spi.WorkerBuildContext;
import org.hibernate.search.spi.impl.IndexedTypeSets;
import org.hibernate.search.util.StringHelper;
import org.hibernate.search.util.configuration.impl.ConfigurationParseHelper;
import org.hibernate.search.util.impl.Closer;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * An {@link IndexManager} applying indexing work to an Elasticsearch server.
 *
 * @author Gunnar Morling
 */
public class ElasticsearchIndexManager implements IndexManager, IndexNameNormalizer {

	static final Log LOG = LoggerFactory.make( Log.class );

	/**
	 * The index name for Hibernate Search, which is actually
	 * the index <em>manager</em> name.
	 * <p>
	 * Following the behavior of Lucene index managers, this
	 * name will reflect any annotation-based index name override
	 * ({@code @Indexed(index = "override")}) but will ignore
	 * configuration-based override
	 * ({@code hibernate.search.my.package.MyClass.indexName = foo}),
	 * which is only reflected in {@link #actualIndexName}.
	 */
	private String indexName;

	/**
	 * The index name for the Elasticsearch module, i.e. the
	 * actual name of the underlying Elasticsearch index.
	 */
	private URLEncodedString actualIndexName;

	private boolean refreshAfterWrite;
	private boolean sync;
	private IndexSchemaManagementStrategy schemaManagementStrategy;
	private ExecutionOptions schemaManagementExecutionOptions;

	private Similarity similarity;

	private ExtendedSearchIntegrator searchIntegrator;
	private IndexedTypeSet containedEntityTypes = IndexedTypeSets.empty();

	private boolean indexInitialized = false;
	private boolean indexCreatedByHibernateSearch = false;
	private IndexedTypeSet initializedContainedEntityTypes = IndexedTypeSets.empty();

	private ServiceManager serviceManager;

	private ElasticsearchService elasticsearchService;
	private ElasticsearchIndexWorkVisitor visitor;
	private ElasticsearchWorkProcessor workProcessor;
	private BarrierElasticsearchWorkOrchestrator nonStreamOrchestrator;

	// Lifecycle

	@Override
	public void initialize(String indexName, Properties properties, Similarity similarity, WorkerBuildContext context) {
		this.serviceManager = context.getServiceManager();

		this.indexName = indexName;
		this.schemaManagementStrategy = getIndexManagementStrategy( properties );
		final ElasticsearchIndexStatus requiredIndexStatus = getRequiredIndexStatus( properties );
		final int indexManagementWaitTimeout = getIndexManagementWaitTimeout( properties );
		final boolean multitenancyEnabled = context.isMultitenancyEnabled();
		final DynamicType dynamicMapping = getDynamicMapping( properties );

		this.schemaManagementExecutionOptions = new ExecutionOptions() {
			@Override
			public ElasticsearchIndexStatus getRequiredIndexStatus() {
				return requiredIndexStatus;
			}

			@Override
			public int getIndexManagementTimeoutInMs() {
				return indexManagementWaitTimeout;
			}

			@Override
			public boolean isMultitenancyEnabled() {
				return multitenancyEnabled;
			}

			@Override
			public DynamicType getDynamicMapping() {
				return dynamicMapping;
			}
		};

		String overriddenIndexName = getOverriddenIndexName( indexName, properties );
		this.actualIndexName = ElasticsearchIndexNameNormalizer.getElasticsearchIndexName( overriddenIndexName );
		this.refreshAfterWrite = getRefreshAfterWrite( properties );
		this.sync = BackendFactory.isConfiguredAsSync( properties );

		this.similarity = similarity;

		this.elasticsearchService = serviceManager.requestService( ElasticsearchService.class );
		this.visitor = new ElasticsearchIndexWorkVisitor(
				this.actualIndexName,
				this.refreshAfterWrite,
				context.getUninitializedSearchIntegrator(),
				elasticsearchService.getWorkFactory()
		);
		this.workProcessor = elasticsearchService.getWorkProcessor();

		this.nonStreamOrchestrator = workProcessor.createNonStreamOrchestrator( indexName, refreshAfterWrite );
	}

	/**
	 * @return the ElasticsearchService used by this index manager.
	 */
	public ElasticsearchService getElasticsearchService() {
		return elasticsearchService;
	}

	private static String getOverriddenIndexName(String indexName, Properties properties) {
		String name = properties.getProperty( Environment.INDEX_NAME_PROP_NAME );
		return name != null ? name : indexName;
	}

	private static IndexSchemaManagementStrategy getIndexManagementStrategy(Properties properties) {
		String propertyValue = properties.getProperty( ElasticsearchEnvironment.INDEX_SCHEMA_MANAGEMENT_STRATEGY );
		if ( StringHelper.isNotEmpty( propertyValue ) ) {
			return IndexSchemaManagementStrategy.interpretPropertyValue( propertyValue );
		}
		else {
			return ElasticsearchEnvironment.Defaults.INDEX_SCHEMA_MANAGEMENT_STRATEGY;
		}
	}

	private static int getIndexManagementWaitTimeout(Properties properties) {
		int timeout = ConfigurationParseHelper.getIntValue(
				properties,
				ElasticsearchEnvironment.INDEX_MANAGEMENT_WAIT_TIMEOUT,
				ElasticsearchEnvironment.Defaults.INDEX_MANAGEMENT_WAIT_TIMEOUT
		);

		if ( timeout < 0 ) {
			throw LOG.negativeTimeoutValue( timeout );
		}

		return timeout;
	}

	private static DynamicType getDynamicMapping(Properties properties) {
		String status = ConfigurationParseHelper.getString(
				properties,
				ElasticsearchEnvironment.DYNAMIC_MAPPING,
				ElasticsearchEnvironment.Defaults.DYNAMIC_MAPPING.name() );

		return DynamicType.valueOf( status.toUpperCase( Locale.ROOT ) );
	}

	private static ElasticsearchIndexStatus getRequiredIndexStatus(Properties properties) {
		String status = ConfigurationParseHelper.getString(
				properties,
				ElasticsearchEnvironment.REQUIRED_INDEX_STATUS,
				null
		);

		if ( status == null ) {
			return ElasticsearchEnvironment.Defaults.REQUIRED_INDEX_STATUS;
		}
		else {
			return ElasticsearchIndexStatus.fromString( status );
		}
	}

	private static boolean getRefreshAfterWrite(Properties properties) {
		return ConfigurationParseHelper.getBooleanValue(
				properties,
				ElasticsearchEnvironment.REFRESH_AFTER_WRITE,
				ElasticsearchEnvironment.Defaults.REFRESH_AFTER_WRITE
		);
	}

	@Override
	public void destroy() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.push( this::awaitCompletion, nonStreamOrchestrator );
			if ( schemaManagementStrategy == IndexSchemaManagementStrategy.DROP_AND_CREATE_AND_DROP ) {
				closer.push( () -> elasticsearchService.getSchemaDropper().dropIfExisting( actualIndexName, schemaManagementExecutionOptions ) );
			}
			closer.push( nonStreamOrchestrator::close );

			workProcessor = null;
			visitor = null;
			elasticsearchService = null;
			closer.push( serviceManager::releaseService, ElasticsearchService.class );

			schemaManagementExecutionOptions = null;

			serviceManager = null;
		}
	}

	@Override
	public void setSearchFactory(ExtendedSearchIntegrator boundSearchIntegrator) {
		this.searchIntegrator = boundSearchIntegrator;
		initializeIndex();
	}

	private void initializeIndex() {
		if ( !indexInitialized ) {
			/*
			 * The value of this variable is only used for the "CREATE" schema management
			 * strategy, but we store it in any case, just to be consistent.
			 */
			indexCreatedByHibernateSearch = initializeIndex( containedEntityTypes );
			indexInitialized = true;
			initializedContainedEntityTypes = IndexedTypeSets.composite( initializedContainedEntityTypes, containedEntityTypes );
		}
		else {
			IndexedTypeSet notYetInitializedContainedEntityTypes = IndexedTypeSets.subtraction( containedEntityTypes, initializedContainedEntityTypes );

			if ( notYetInitializedContainedEntityTypes.isEmpty() ) {
				return; // Nothing to do
			}

			reinitializeIndex( notYetInitializedContainedEntityTypes );
			initializedContainedEntityTypes = IndexedTypeSets.composite( initializedContainedEntityTypes, notYetInitializedContainedEntityTypes );
		}
	}

	/**
	 * Called only the first time we must initialize the index.
	 *
	 * @param entityTypesToInitialize The entity types whose mapping will be added to the index
	 * (if it's part of the schema management strategy).
	 * @return {@code true} if the index had to be created, {@code false} otherwise.
	 */
	private boolean initializeIndex(IndexedTypeSet entityTypesToInitialize) {
		if ( schemaManagementStrategy == IndexSchemaManagementStrategy.NONE ) {
			return false;
		}

		boolean createdIndex;

		ElasticsearchSchemaCreator schemaCreator = elasticsearchService.getSchemaCreator();

		IndexMetadata indexMetadata = createIndexMetadata( entityTypesToInitialize );
		switch ( schemaManagementStrategy ) {
			case CREATE:
				createdIndex = schemaCreator.createIndexIfAbsent( indexMetadata, schemaManagementExecutionOptions );
				if ( createdIndex ) {
					schemaCreator.createMappings( indexMetadata, schemaManagementExecutionOptions );
				}
				break;
			case DROP_AND_CREATE:
			case DROP_AND_CREATE_AND_DROP:
				ElasticsearchSchemaDropper schemaDropper = elasticsearchService.getSchemaDropper();
				schemaDropper.dropIfExisting( actualIndexName, schemaManagementExecutionOptions );
				schemaCreator.createIndex( indexMetadata, schemaManagementExecutionOptions );
				schemaCreator.createMappings( indexMetadata, schemaManagementExecutionOptions );
				createdIndex = true;
				break;
			case UPDATE:
				createdIndex = schemaCreator.createIndexIfAbsent( indexMetadata, schemaManagementExecutionOptions );
				if ( createdIndex ) {
					schemaCreator.createMappings( indexMetadata, schemaManagementExecutionOptions );
				}
				else {
					ElasticsearchSchemaMigrator schemaMigrator = elasticsearchService.getSchemaMigrator();
					schemaMigrator.migrate( indexMetadata, schemaManagementExecutionOptions );
				}
				break;
			case VALIDATE:
				ElasticsearchSchemaValidator schemaValidator = elasticsearchService.getSchemaValidator();
				schemaCreator.checkIndexExists( actualIndexName, schemaManagementExecutionOptions );
				schemaValidator.validate( indexMetadata, schemaManagementExecutionOptions );
				createdIndex = false;
				break;
			default:
				throw new AssertionFailure( "Unexpected schema management strategy: " + schemaManagementStrategy );
		}

		return createdIndex;
	}

	/**
	 * Called for any initialization following the {@link #initialize(String, Properties, Similarity, WorkerBuildContext) first one}
	 * (upon subsequent search factory changes).
	 *
	 * <p>This method only may add new mappings to the existing index (depending on the strategy), but will never
	 * create or drop the index (since it's supposed to have been created by Hibernate Search already, if necessary).
	 *
	 * @param indexCreatedByHibernateSearch If the index was created by Hibernate Search in {@link #initializeIndex(Set)}.
	 * @param entityTypesToInitialize The entity types whose mapping will be added to the index
	 * (if it's part of the schema management strategy).
	 */
	private void reinitializeIndex(IndexedTypeSet entityTypesToInitialize) {
		if ( schemaManagementStrategy == IndexSchemaManagementStrategy.NONE ) {
			return;
		}

		ElasticsearchSchemaCreator schemaCreator = elasticsearchService.getSchemaCreator();

		IndexMetadata indexMetadata = createIndexMetadata( entityTypesToInitialize );
		switch ( schemaManagementStrategy ) {
			case CREATE:
				if ( indexCreatedByHibernateSearch ) { // Don't alter a pre-existing index
					schemaCreator.createMappings( indexMetadata, schemaManagementExecutionOptions );
				}
				break;
			case DROP_AND_CREATE:
			case DROP_AND_CREATE_AND_DROP:
				schemaCreator.createMappings( indexMetadata, schemaManagementExecutionOptions );
				break;
			case UPDATE:
				ElasticsearchSchemaMigrator schemaMigrator = elasticsearchService.getSchemaMigrator();
				schemaMigrator.migrate( indexMetadata, schemaManagementExecutionOptions );
				break;
			case VALIDATE:
				ElasticsearchSchemaValidator schemaValidator = elasticsearchService.getSchemaValidator();
				schemaValidator.validate( indexMetadata, schemaManagementExecutionOptions );
				break;
			default:
				throw new AssertionFailure( "Unexpected schema management strategy: " + schemaManagementStrategy );
		}
	}

	private IndexMetadata createIndexMetadata(IndexedTypeSet entityTypes) {
		List<EntityIndexBinding> descriptors = new ArrayList<>();
		for ( IndexedTypeIdentifier entityType : entityTypes ) {
			EntityIndexBinding descriptor = searchIntegrator.getIndexBinding( entityType );
			descriptors.add( descriptor );
		}

		ElasticsearchSchemaTranslator schemaTranslator = elasticsearchService.getSchemaTranslator();
		return schemaTranslator.translate( actualIndexName, descriptors, schemaManagementExecutionOptions );
	}

	@Override
	public void addContainedEntity(IndexedTypeIdentifier entity) {
		containedEntityTypes = IndexedTypeSets.composite( containedEntityTypes, entity );
	}

	// Getters

	@Override
	public String getIndexName() {
		return indexName;
	}

	@Override
	public ReaderProvider getReaderProvider() {
		throw LOG.indexManagerReaderProviderUnsupported();
	}

	@Override
	public IndexedTypeSet getContainedTypes() {
		return containedEntityTypes;
	}

	@Override
	public Similarity getSimilarity() {
		return similarity;
	}

	@Override
	public Analyzer getAnalyzer(String name) {
		return searchIntegrator.getAnalyzer( name );
	}

	@Override
	public LuceneWorkSerializer getSerializer() {
		return null;
	}

	@Override
	public void flushAndReleaseResources() {
		ElasticsearchWork<?> flushWork = elasticsearchService.getWorkFactory().flush()
				.index( actualIndexName )
				.build();
		awaitCompletion( nonStreamOrchestrator );
		awaitCompletion( workProcessor.getStreamOrchestrator() );
		nonStreamOrchestrator
				.submit( flushWork )
				.join();
	}

	@Override
	public String getActualIndexName() {
		return actualIndexName.original;
	}

	// Runtime ops

	@Override
	public void performOperations(List<LuceneWork> workList, IndexingMonitor monitor) {
		List<ElasticsearchWork<?>> elasticsearchWorks = new ArrayList<>( workList.size() );
		for ( LuceneWork luceneWork : workList ) {
			elasticsearchWorks.add( luceneWork.acceptIndexWorkVisitor( visitor, monitor ) );
		}

		CompletableFuture<?> future = nonStreamOrchestrator.submit( elasticsearchWorks );
		if ( sync ) {
			future.join();
		}
	}

	@Override
	public void performStreamOperation(LuceneWork singleOperation, IndexingMonitor monitor, boolean forceAsync) {
		ElasticsearchWork<?> elasticsearchWork = singleOperation.acceptIndexWorkVisitor( visitor, monitor );
		if ( singleOperation instanceof FlushLuceneWork ) {
			awaitAsyncProcessingCompletion();
			workProcessor.getStreamOrchestrator()
					.submit( elasticsearchWork )
					.join();
		}
		else {
			workProcessor.getStreamOrchestrator()
					.submit( elasticsearchWork );
		}
	}

	@Override
	public void awaitAsyncProcessingCompletion() {
		if ( !sync ) {
			awaitCompletion( nonStreamOrchestrator );
		}
		awaitCompletion( workProcessor.getStreamOrchestrator() );
	}

	private void awaitCompletion(BarrierElasticsearchWorkOrchestrator orchestrator) {
		try {
			orchestrator.awaitCompletion();
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw LOG.interruptedWhileWaitingForRequestCompletion( e );
		}
	}

	@Override
	public void optimize() {
		performStreamOperation( OptimizeLuceneWork.INSTANCE, null, false );
	}

	@Override
	public String toString() {
		return "ElasticsearchIndexManager [actualIndexName=" + actualIndexName + "]";
	}

	@Override
	public IndexManagerType getIndexManagerType() {
		return ElasticsearchIndexManagerType.INSTANCE;
	}

}
