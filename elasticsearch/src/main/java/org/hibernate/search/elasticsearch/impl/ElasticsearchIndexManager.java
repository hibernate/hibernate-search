/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.similarities.Similarity;
import org.hibernate.search.backend.FlushLuceneWork;
import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.OptimizeLuceneWork;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.elasticsearch.cfg.ElasticsearchEnvironment;
import org.hibernate.search.elasticsearch.cfg.ElasticsearchIndexStatus;
import org.hibernate.search.elasticsearch.cfg.IndexSchemaManagementStrategy;
import org.hibernate.search.elasticsearch.client.impl.BackendRequest;
import org.hibernate.search.elasticsearch.client.impl.BackendRequestProcessor;
import org.hibernate.search.elasticsearch.logging.impl.Log;
import org.hibernate.search.elasticsearch.schema.impl.ElasticsearchSchemaCreator;
import org.hibernate.search.elasticsearch.schema.impl.ElasticsearchSchemaDropper;
import org.hibernate.search.elasticsearch.schema.impl.ElasticsearchSchemaMigrator;
import org.hibernate.search.elasticsearch.schema.impl.ElasticsearchSchemaTranslator;
import org.hibernate.search.elasticsearch.schema.impl.ElasticsearchSchemaValidator;
import org.hibernate.search.elasticsearch.schema.impl.ExecutionOptions;
import org.hibernate.search.elasticsearch.schema.impl.model.IndexMetadata;
import org.hibernate.search.elasticsearch.spi.ElasticsearchIndexManagerType;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.indexes.serialization.spi.LuceneWorkSerializer;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.indexes.spi.IndexManagerType;
import org.hibernate.search.indexes.spi.IndexNameNormalizer;
import org.hibernate.search.indexes.spi.ReaderProvider;
import org.hibernate.search.spi.WorkerBuildContext;
import org.hibernate.search.util.configuration.impl.ConfigurationParseHelper;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * An {@link IndexManager} applying indexing work to an Elasticsearch server.
 *
 * @author Gunnar Morling
 */
public class ElasticsearchIndexManager implements IndexManager, IndexNameNormalizer {

	static final Log LOG = LoggerFactory.make( Log.class );

	private String indexName;
	private String actualIndexName;
	private boolean refreshAfterWrite;
	private IndexSchemaManagementStrategy schemaManagementStrategy;
	private ExecutionOptions schemaManagementExecutionOptions;

	private Similarity similarity;

	private ExtendedSearchIntegrator searchIntegrator;
	private final Set<Class<?>> containedEntityTypes = new HashSet<>();

	private boolean indexInitialized = false;
	private boolean indexCreatedByHibernateSearch = false;
	private final Set<Class<?>> initializedContainedEntityTypes = new HashSet<>();

	private ServiceManager serviceManager;

	private ElasticsearchIndexWorkVisitor visitor;
	private BackendRequestProcessor requestProcessor;

	private ElasticsearchSchemaCreator schemaCreator;
	private ElasticsearchSchemaDropper schemaDropper;
	private ElasticsearchSchemaMigrator schemaMigrator;
	private ElasticsearchSchemaValidator schemaValidator;
	private ElasticsearchSchemaTranslator schemaTranslator;

	// Lifecycle

	@Override
	public void initialize(String indexName, Properties properties, Similarity similarity, WorkerBuildContext context) {
		this.serviceManager = context.getServiceManager();

		this.indexName = getIndexName( indexName, properties );
		this.schemaManagementStrategy = getIndexManagementStrategy( properties );
		final ElasticsearchIndexStatus requiredIndexStatus = getRequiredIndexStatus( properties );
		final int indexManagementWaitTimeout = getIndexManagementWaitTimeout( properties );
		final boolean multitenancyEnabled = context.isMultitenancyEnabled();
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
		};
		this.schemaCreator = serviceManager.requestService( ElasticsearchSchemaCreator.class );
		this.schemaDropper = serviceManager.requestService( ElasticsearchSchemaDropper.class );
		this.schemaMigrator = serviceManager.requestService( ElasticsearchSchemaMigrator.class );
		this.schemaValidator = serviceManager.requestService( ElasticsearchSchemaValidator.class );
		this.schemaTranslator = serviceManager.requestService( ElasticsearchSchemaTranslator.class );

		this.actualIndexName = ElasticsearchIndexNameNormalizer.getElasticsearchIndexName( this.indexName );
		this.refreshAfterWrite = getRefreshAfterWrite( properties );

		this.similarity = similarity;

		this.visitor = new ElasticsearchIndexWorkVisitor(
				this.actualIndexName,
				this.refreshAfterWrite,
				context.getUninitializedSearchIntegrator()
		);
		this.requestProcessor = context.getServiceManager().requestService( BackendRequestProcessor.class );
	}

	private static String getIndexName(String indexName, Properties properties) {
		String name = properties.getProperty( Environment.INDEX_NAME_PROP_NAME );
		return name != null ? name : indexName;
	}

	private static IndexSchemaManagementStrategy getIndexManagementStrategy(Properties properties) {
		String strategy = properties.getProperty( ElasticsearchEnvironment.INDEX_SCHEMA_MANAGEMENT_STRATEGY );
		return strategy != null ? IndexSchemaManagementStrategy.valueOf( strategy ) : ElasticsearchEnvironment.Defaults.INDEX_SCHEMA_MANAGEMENT_STRATEGY;
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

	private static ElasticsearchIndexStatus getRequiredIndexStatus(Properties properties) {
		String status = ConfigurationParseHelper.getString(
				properties,
				ElasticsearchEnvironment.REQUIRED_INDEX_STATUS,
				ElasticsearchEnvironment.Defaults.REQUIRED_INDEX_STATUS
		);

		return ElasticsearchIndexStatus.fromString( status );
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
		if ( schemaManagementStrategy == IndexSchemaManagementStrategy.RECREATE_DELETE ) {
			schemaDropper.dropIfExisting( actualIndexName, schemaManagementExecutionOptions );
		}

		requestProcessor = null;
		serviceManager.releaseService( BackendRequestProcessor.class );
		schemaTranslator = null;
		serviceManager.releaseService( ElasticsearchSchemaTranslator.class );
		schemaValidator = null;
		serviceManager.releaseService( ElasticsearchSchemaValidator.class );
		schemaMigrator = null;
		serviceManager.releaseService( ElasticsearchSchemaMigrator.class );
		schemaDropper = null;
		serviceManager.releaseService( ElasticsearchSchemaDropper.class );
		schemaCreator = null;
		serviceManager.releaseService( ElasticsearchSchemaCreator.class );

		schemaManagementExecutionOptions = null;

		serviceManager = null;
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
			initializedContainedEntityTypes.addAll( containedEntityTypes );
		}
		else {
			Set<Class<?>> notYetInitializedContainedEntityTypes = new HashSet<>( containedEntityTypes );
			notYetInitializedContainedEntityTypes.removeAll( initializedContainedEntityTypes );

			if ( notYetInitializedContainedEntityTypes.isEmpty() ) {
				return; // Nothing to do
			}

			reinitializeIndex( notYetInitializedContainedEntityTypes );
			initializedContainedEntityTypes.addAll( notYetInitializedContainedEntityTypes );
		}
	}

	/**
	 * Called only the first time we must initialize the index.
	 *
	 * @param entityTypesToInitialize The entity types whose mapping will be added to the index
	 * (if it's part of the schema management strategy).
	 * @return {@code true} if the index had to be created, {@code false} otherwise.
	 */
	private boolean initializeIndex(Set<Class<?>> entityTypesToInitialize) {
		if ( schemaManagementStrategy == IndexSchemaManagementStrategy.NONE ) {
			return false;
		}

		boolean createdIndex;

		IndexMetadata indexMetadata = createIndexMetadata( entityTypesToInitialize );
		switch ( schemaManagementStrategy ) {
			case CREATE:
				createdIndex = schemaCreator.createIndexIfAbsent( indexMetadata, schemaManagementExecutionOptions );
				if ( createdIndex ) {
					schemaCreator.createMappings( indexMetadata, schemaManagementExecutionOptions );
				}
				break;
			case RECREATE:
			case RECREATE_DELETE:
				schemaDropper.dropIfExisting( actualIndexName, schemaManagementExecutionOptions );
				schemaCreator.createIndex( indexMetadata, schemaManagementExecutionOptions );
				schemaCreator.createMappings( indexMetadata, schemaManagementExecutionOptions );
				createdIndex = true;
				break;
			case MERGE:
				createdIndex = schemaCreator.createIndexIfAbsent( indexMetadata, schemaManagementExecutionOptions );
				schemaMigrator.merge( indexMetadata, schemaManagementExecutionOptions );
				break;
			case VALIDATE:
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
	private void reinitializeIndex(Set<Class<?>> entityTypesToInitialize) {
		if ( schemaManagementStrategy == IndexSchemaManagementStrategy.NONE ) {
			return;
		}
		IndexMetadata indexMetadata = createIndexMetadata( entityTypesToInitialize );
		switch ( schemaManagementStrategy ) {
			case CREATE:
				if ( indexCreatedByHibernateSearch ) { // Don't alter a pre-existing index
					schemaCreator.createMappings( indexMetadata, schemaManagementExecutionOptions );
				}
				break;
			case RECREATE:
			case RECREATE_DELETE:
				schemaCreator.createMappings( indexMetadata, schemaManagementExecutionOptions );
				break;
			case MERGE:
				schemaMigrator.merge( indexMetadata, schemaManagementExecutionOptions );
				break;
			case VALIDATE:
				schemaValidator.validate( indexMetadata, schemaManagementExecutionOptions );
				break;
			default:
				throw new AssertionFailure( "Unexpected schema management strategy: " + schemaManagementStrategy );
		}
	}

	private IndexMetadata createIndexMetadata(Collection<Class<?>> classes) {
		IndexMetadata index = new IndexMetadata();
		index.setName( actualIndexName );
		for ( Class<?> entityType : classes ) {
			String entityName = entityType.getName();
			EntityIndexBinding descriptor = searchIntegrator.getIndexBinding( entityType );
			index.putMapping( entityName, schemaTranslator.translate( descriptor, schemaManagementExecutionOptions ) );
		}
		return index;
	}

	@Override
	public void addContainedEntity(Class<?> entity) {
		containedEntityTypes.add( entity );
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
	public Set<Class<?>> getContainedTypes() {
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
		// no-op
	}

	@Override
	public String getActualIndexName() {
		return actualIndexName;
	}

	public boolean needsRefreshAfterWrite() {
		return refreshAfterWrite;
	}

	// Runtime ops

	@Override
	public void performOperations(List<LuceneWork> workList, IndexingMonitor monitor) {
		List<BackendRequest<?>> requests = new ArrayList<>( workList.size() );
		for ( LuceneWork luceneWork : workList ) {
			requests.add( luceneWork.acceptIndexWorkVisitor( visitor, monitor ) );
		}

		requestProcessor.executeSync( requests );
	}

	@Override
	public void performStreamOperation(LuceneWork singleOperation, IndexingMonitor monitor, boolean forceAsync) {
		if ( singleOperation == FlushLuceneWork.INSTANCE ) {
			requestProcessor.awaitAsyncProcessingCompletion();
		}
		else {
			BackendRequest<?> request = singleOperation.acceptIndexWorkVisitor( visitor, monitor );

			if ( request != null ) {
				requestProcessor.executeAsync( request );
			}
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
