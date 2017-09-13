/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.impl;

import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.Query;
import org.hibernate.search.analyzer.spi.AnalyzerReference;
import org.hibernate.search.analyzer.spi.ScopedAnalyzerReference;
import org.hibernate.search.analyzer.impl.LuceneAnalyzerReference;
import org.hibernate.search.backend.impl.TransactionalOperationDispatcher;
import org.hibernate.search.backend.impl.batch.DefaultBatchBackend;
import org.hibernate.search.backend.spi.BatchBackend;
import org.hibernate.search.backend.spi.OperationDispatcher;
import org.hibernate.search.backend.spi.Worker;
import org.hibernate.search.batchindexing.MassIndexerProgressMonitor;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.cfg.SearchMapping;
import org.hibernate.search.cfg.spi.IndexManagerFactory;
import org.hibernate.search.engine.Version;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.engine.integration.impl.SearchIntegration;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.engine.service.spi.ServiceReference;
import org.hibernate.search.engine.spi.DocumentBuilderContainedEntity;
import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.engine.spi.TimingSource;
import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.exception.ErrorHandler;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.filter.FilterCachingStrategy;
import org.hibernate.search.indexes.IndexReaderAccessor;
import org.hibernate.search.indexes.impl.DefaultIndexReaderAccessor;
import org.hibernate.search.indexes.impl.IndexManagerHolder;
import org.hibernate.search.indexes.serialization.spi.LuceneWorkSerializer;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.indexes.spi.IndexManagerType;
import org.hibernate.search.indexes.spi.LuceneEmbeddedIndexManagerType;
import org.hibernate.search.jmx.StatisticsInfoMBean;
import org.hibernate.search.util.jmx.impl.JMXRegistrar;
import org.hibernate.search.metadata.IndexedTypeDescriptor;
import org.hibernate.search.metadata.impl.IndexedTypeDescriptorForUnindexedType;
import org.hibernate.search.metadata.impl.IndexedTypeDescriptorImpl;
import org.hibernate.search.query.DatabaseRetrievalMethod;
import org.hibernate.search.query.ObjectLookupMethod;
import org.hibernate.search.query.dsl.QueryContextBuilder;
import org.hibernate.search.query.dsl.impl.ConnectedQueryContextBuilder;
import org.hibernate.search.query.engine.impl.LuceneQueryTranslator;
import org.hibernate.search.query.engine.spi.HSQuery;
import org.hibernate.search.query.engine.spi.QueryDescriptor;
import org.hibernate.search.query.engine.spi.TimeoutExceptionFactory;
import org.hibernate.search.spi.CustomTypeMetadata;
import org.hibernate.search.spi.IndexedTypeIdentifier;
import org.hibernate.search.spi.IndexedTypeSet;
import org.hibernate.search.spi.IndexingMode;
import org.hibernate.search.spi.InstanceInitializer;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.spi.IndexedTypeMap;
import org.hibernate.search.spi.WorkerBuildContext;
import org.hibernate.search.spi.impl.ExtendedSearchIntegratorWithShareableState;
import org.hibernate.search.spi.impl.IndexedTypeMaps;
import org.hibernate.search.spi.impl.IndexedTypeSets;
import org.hibernate.search.spi.impl.TypeHierarchy;
import org.hibernate.search.spi.impl.SearchFactoryState;
import org.hibernate.search.stat.Statistics;
import org.hibernate.search.stat.impl.StatisticsImpl;
import org.hibernate.search.stat.spi.StatisticsImplementor;
import org.hibernate.search.util.configuration.impl.ConfigurationParseHelper;
import org.hibernate.search.util.impl.Closer;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * This implementation is never directly exposed to the user, it is always wrapped into a {@link org.hibernate.search.engine.impl.MutableSearchFactory}
 *
 * @author Emmanuel Bernard
 */
public class ImmutableSearchFactory implements ExtendedSearchIntegratorWithShareableState, WorkerBuildContext {

	static {
		Version.touch();
	}

	private static final Log log = LoggerFactory.make();

	private final IndexedTypeMap<EntityIndexBinding> indexBindingForEntities;
	private final IndexedTypeMap<DocumentBuilderContainedEntity> documentBuildersContainedEntities;
	/**
	 * Lazily populated map of type descriptors
	 */
	private final IndexedTypeMap<IndexedTypeDescriptor> indexedTypeDescriptors;
	private final Worker worker;
	private final Map<String, FilterDef> filterDefinitions;
	private final FilterCachingStrategy filterCachingStrategy;
	private final Map<IndexManagerType, SearchIntegration> integrations;
	private final AtomicBoolean stopped = new AtomicBoolean( false );
	private final int cacheBitResultsSize;
	private final Properties configurationProperties;
	private final TypeHierarchy configuredTypeHierarchy;
	private final TypeHierarchy indexedTypeHierarchy;
	private final StatisticsImpl statistics;
	private final boolean transactionManagerExpected;
	private final IndexManagerHolder allIndexesManager;
	private final ErrorHandler errorHandler;
	private final IndexingMode indexingMode;
	private final ServiceManager serviceManager;
	/**
	 * A flag avoiding unnecessary service lookups when there's no query translator.
	 */
	private final boolean queryTranslatorPresent;
	private final boolean enableDirtyChecks;
	private final DefaultIndexReaderAccessor indexReaderAccessor;
	private final InstanceInitializer instanceInitializer;
	private final TimeoutExceptionFactory timeoutExceptionFactory;
	private final TimingSource timingSource;
	private final SearchMapping mapping;
	private final boolean indexMetadataIsComplete;
	private final boolean isDeleteByTermEnforced;
	private final boolean isIdProvidedImplicit;
	private final boolean isMultitenancyEnabled;
	private final String statisticsMBeanName;
	private final IndexManagerFactory indexManagerFactory;
	private final ObjectLookupMethod defaultObjectLookupMethod;
	private final DatabaseRetrievalMethod defaultDatabaseRetrievalMethod;
	private final boolean enlistWorkerInTransaction;
	private final boolean indexUninvertingAllowed;
	private volatile LuceneWorkSerializer workSerializer;

	public ImmutableSearchFactory(SearchFactoryState state) {
		this.integrations = state.getIntegrations();
		this.cacheBitResultsSize = state.getCacheBitResultsSize();
		this.configurationProperties = state.getConfigurationProperties();
		this.indexBindingForEntities = state.getIndexBindings();
		this.documentBuildersContainedEntities = state.getDocumentBuildersContainedEntities();
		this.filterCachingStrategy = state.getFilterCachingStrategy();
		this.filterDefinitions = state.getFilterDefinitions();
		this.configuredTypeHierarchy = state.getConfiguredTypeHierarchy();
		this.indexedTypeHierarchy = state.getIndexedTypeHierarchy();
		this.indexingMode = state.getIndexingMode();
		this.worker = state.getWorker();
		this.serviceManager = state.getServiceManager();
		this.queryTranslatorPresent = determineQueryTranslatorPresent();
		this.transactionManagerExpected = state.isTransactionManagerExpected();
		this.allIndexesManager = state.getAllIndexesManager();
		this.errorHandler = state.getErrorHandler();
		this.instanceInitializer = state.getInstanceInitializer();
		this.timeoutExceptionFactory = state.getDefaultTimeoutExceptionFactory();
		this.timingSource = state.getTimingSource();
		this.mapping = state.getProgrammaticMapping();
		if ( state.getStatistics() == null ) {
			this.statistics = new StatisticsImpl( this );
			boolean statsEnabled = ConfigurationParseHelper.getBooleanValue(
					configurationProperties, Environment.GENERATE_STATS, false
			);
			this.statistics.setStatisticsEnabled( statsEnabled );
		}
		else {
			this.statistics = (StatisticsImpl) state.getStatistics();
		}
		this.indexMetadataIsComplete = state.isIndexMetadataComplete();
		this.isDeleteByTermEnforced = state.isDeleteByTermEnforced();
		this.isIdProvidedImplicit = state.isIdProvidedImplicit();
		this.isMultitenancyEnabled = state.isMultitenancyEnabled();
		this.indexManagerFactory = state.getIndexManagerFactory();
		this.workSerializer = state.getWorkSerializerState();

		this.enableDirtyChecks = ConfigurationParseHelper.getBooleanValue(
				configurationProperties, Environment.ENABLE_DIRTY_CHECK, true
		);

		if ( isJMXEnabled() ) {
			this.statisticsMBeanName = registerMBeans();
		}
		else {
			this.statisticsMBeanName = null;
		}

		this.indexReaderAccessor = new DefaultIndexReaderAccessor( this );
		this.indexedTypeDescriptors = IndexedTypeMaps.concurrentHashMap();

		this.defaultObjectLookupMethod = determineDefaultObjectLookupMethod();
		this.defaultDatabaseRetrievalMethod = determineDefaultDatabaseRetrievalMethod();
		this.enlistWorkerInTransaction = ConfigurationParseHelper.getBooleanValue(
				configurationProperties, Environment.WORKER_ENLIST_IN_TRANSACTION, false
		);

		this.indexUninvertingAllowed = ConfigurationParseHelper.getBooleanValue(
				configurationProperties, Environment.INDEX_UNINVERTING_ALLOWED, false
		);
	}

	private ObjectLookupMethod determineDefaultObjectLookupMethod() {
		String objectLookupMethod = configurationProperties.getProperty( Environment.OBJECT_LOOKUP_METHOD );
		if ( objectLookupMethod == null ) {
			return ObjectLookupMethod.SKIP; // default
		}
		else {
			try {
				return Enum.valueOf( ObjectLookupMethod.class, objectLookupMethod.toUpperCase( Locale.ROOT ) );
			}
			catch (IllegalArgumentException e) {
				throw log.invalidPropertyValue( objectLookupMethod, Environment.OBJECT_LOOKUP_METHOD );
			}
		}
	}

	private boolean determineQueryTranslatorPresent() {
		try (ServiceReference<LuceneQueryTranslator> translator =
				getServiceManager().requestReference( LuceneQueryTranslator.class )) {
			return true;
		}
		catch (Exception e) {
			// Ignore
			return false;
		}
	}

	private DatabaseRetrievalMethod determineDefaultDatabaseRetrievalMethod() {
		String databaseRetrievalMethod = configurationProperties.getProperty( Environment.DATABASE_RETRIEVAL_METHOD );
		if ( databaseRetrievalMethod == null ) {
			return DatabaseRetrievalMethod.QUERY; // default
		}
		else {
			try {
				return Enum.valueOf( DatabaseRetrievalMethod.class, databaseRetrievalMethod.toUpperCase( Locale.ROOT ) );
			}
			catch (IllegalArgumentException e) {
				throw log.invalidPropertyValue( databaseRetrievalMethod, Environment.OBJECT_LOOKUP_METHOD );
			}
		}
	}

	@Override
	public Map<String, FilterDef> getFilterDefinitions() {
		return filterDefinitions;
	}

	@Override
	public IndexingMode getIndexingMode() {
		return indexingMode;
	}

	@Override
	public void close() {
		if ( stopped.compareAndSet( false, true ) ) { //make sure we only stop once
			try ( Closer<RuntimeException> closer = new Closer<>() ) {
				closer.push( () -> {
					try {
						worker.close();
					}
					catch (Exception e) {
						log.workerException( e );
					}
				} );

				closer.push( allIndexesManager::stop );
				closer.push( timingSource::stop );

				if ( workSerializer != null ) {
					closer.push( serviceManager::releaseService, LuceneWorkSerializer.class );
				}

				closer.push( serviceManager::releaseAllServices );
				closer.pushAll( SearchIntegration::close, this.integrations.values() );

				// unregister statistic mbean
				if ( statisticsMBeanName != null ) {
					closer.push( JMXRegistrar::unRegisterMBean, statisticsMBeanName );
				}
			}
		}
	}

	@Override
	public HSQuery createHSQuery(Query luceneQuery, Class<?>... entityTypes) {
		IndexedTypeSet newtypes = IndexedTypeSets.fromClasses( entityTypes );
		QueryDescriptor descriptor = createQueryDescriptor( luceneQuery, newtypes );
		return descriptor.createHSQuery( this, newtypes );
	}

	@Override
	public HSQuery createHSQuery(Query luceneQuery, IndexedTypeMap<CustomTypeMetadata> types) {
		IndexedTypeSet entityTypes = types.keySet();
		QueryDescriptor descriptor = createQueryDescriptor( luceneQuery, entityTypes );
		return descriptor.createHSQuery( this, types );
	}

	private QueryDescriptor createQueryDescriptor(Query luceneQuery, IndexedTypeSet entityTypes) {
		QueryDescriptor descriptor = null;

		if ( queryTranslatorPresent ) {
			try (ServiceReference<LuceneQueryTranslator> translator =
					getServiceManager().requestReference( LuceneQueryTranslator.class )) {
				if ( translator.get().conversionRequired( entityTypes ) ) {
					descriptor = translator.get().convertLuceneQuery( luceneQuery );
				}
			}
		}

		if ( descriptor == null ) {
			descriptor = new LuceneQueryDescriptor( luceneQuery );
		}

		return descriptor;
	}

	@Override
	public IndexedTypeMap<DocumentBuilderContainedEntity> getDocumentBuildersContainedEntities() {
		return documentBuildersContainedEntities;
	}

	@Override
	public IndexedTypeMap<EntityIndexBinding> getIndexBindings() {
		return indexBindingForEntities;
	}

	@Override
	public EntityIndexBinding getIndexBinding(IndexedTypeIdentifier entityType) {
		return indexBindingForEntities.get( entityType );
	}

	@Override
	public DocumentBuilderContainedEntity getDocumentBuilderContainedEntity(IndexedTypeIdentifier entityType) {
		return documentBuildersContainedEntities.get( entityType );
	}

	@Override
	public void addClasses(Class<?>... classes) {
		throw new AssertionFailure( "Cannot add classes to an " + ImmutableSearchFactory.class.getName() );
	}

	@Override
	public Worker getWorker() {
		return worker;
	}

	@Override
	public void optimize() {
		for ( IndexManager im : this.allIndexesManager.getIndexManagers() ) {
			im.optimize();
		}
	}

	@Override
	public void optimize(IndexedTypeIdentifier entityType) {
		EntityIndexBinding entityIndexBinding = getSafeIndexBindingForEntity( entityType );
		for ( IndexManager im : entityIndexBinding.getIndexManagerSelector().all() ) {
			im.optimize();
		}
	}

	@Override
	// This method is a bit convoluted but it is going to be removed
	// At the moment we cannot change this API because it's public
	public Analyzer getAnalyzer(String name) {
		final SearchIntegration integration = integrations.get( LuceneEmbeddedIndexManagerType.INSTANCE );
		final AnalyzerRegistry registry = integration.getAnalyzerRegistry();
		if ( registry == null ) {
			throw new SearchException( "Unknown Analyzer definition: " + name );
		}
		final AnalyzerReference reference = registry.getAnalyzerReference( name );
		if ( reference == null || !reference.is( LuceneAnalyzerReference.class ) ) {
			throw new SearchException( "Unknown Analyzer definition: " + name );
		}
		Analyzer analyzer = reference.unwrap( LuceneAnalyzerReference.class ).getAnalyzer();
		if ( analyzer == null ) {
			throw new SearchException( "Unknown Analyzer definition: " + name );
		}
		return analyzer;
	}

	@Override
	public SearchIntegration getIntegration(IndexManagerType indexManagerType) {
		final SearchIntegration integration = integrations.get( indexManagerType );
		if ( integration == null ) {
			throw new SearchException( "Unknown index manager type: " + indexManagerType );
		}
		return integration;
	}

	@Override
	public Analyzer getAnalyzer(IndexedTypeIdentifier type) {
		return getAnalyzerReference( type ).unwrap( LuceneAnalyzerReference.class ).getAnalyzer();
	}

	@Override
	public ScopedAnalyzerReference getAnalyzerReference(IndexedTypeIdentifier type) {
		EntityIndexBinding entityIndexBinding = getSafeIndexBindingForEntity( type );
		DocumentBuilderIndexedEntity builder = entityIndexBinding.getDocumentBuilder();
		return builder.getAnalyzerReference();
	}

	@Override
	public QueryContextBuilder buildQueryBuilder() {
		return new ConnectedQueryContextBuilder( this );
	}

	@Override
	public Statistics getStatistics() {
		return statistics;
	}

	@Override
	public StatisticsImplementor getStatisticsImplementor() {
		return statistics;
	}

	@Override
	public FilterCachingStrategy getFilterCachingStrategy() {
		return filterCachingStrategy;
	}

	@Override
	public Map<IndexManagerType, SearchIntegration> getIntegrations() {
		return integrations;
	}

	@Override
	public int getCacheBitResultsSize() {
		return cacheBitResultsSize;
	}

	@Override
	public Properties getConfigurationProperties() {
		return configurationProperties;
	}

	@Override
	public FilterDef getFilterDefinition(String name) {
		return filterDefinitions.get( name );
	}

	@Override
	public int getFilterCacheBitResultsSize() {
		return cacheBitResultsSize;
	}

	@Override
	public IndexedTypeSet getConfiguredTypesPolymorphic(IndexedTypeSet types) {
		return configuredTypeHierarchy.getConfiguredClasses( types );
	}

	@Override
	public IndexedTypeSet getIndexedTypesPolymorphic(IndexedTypeSet types) {
		return indexedTypeHierarchy.getConfiguredClasses( types );
	}

	@Override
	public BatchBackend makeBatchBackend(MassIndexerProgressMonitor progressMonitor) {
		return new DefaultBatchBackend( this, progressMonitor );
	}

	@Override
	public TypeHierarchy getConfiguredTypeHierarchy() {
		return configuredTypeHierarchy;
	}

	@Override
	public TypeHierarchy getIndexedTypeHierarchy() {
		return indexedTypeHierarchy;
	}

	@Override
	public ServiceManager getServiceManager() {
		return serviceManager;
	}

	@Override
	public DatabaseRetrievalMethod getDefaultDatabaseRetrievalMethod() {
		return defaultDatabaseRetrievalMethod;
	}

	@Override
	public ObjectLookupMethod getDefaultObjectLookupMethod() {
		return defaultObjectLookupMethod;
	}

	@Override
	public ExtendedSearchIntegrator getUninitializedSearchIntegrator() {
		return this;
	}

	@Override
	public boolean isJMXEnabled() {
		String enableJMX = getConfigurationProperties().getProperty( Environment.JMX_ENABLED );
		return "true".equalsIgnoreCase( enableJMX );
	}

	private String registerMBeans() {
		String mbeanNameSuffix = getConfigurationProperties().getProperty( Environment.JMX_BEAN_SUFFIX );
		String objectName = JMXRegistrar.buildMBeanName(
				StatisticsInfoMBean.STATISTICS_MBEAN_OBJECT_NAME,
				mbeanNameSuffix
		);

		// since the SearchIntegrator is mutable we might have an already existing MBean which we have to unregister first
		if ( JMXRegistrar.isNameRegistered( objectName ) ) {
			JMXRegistrar.unRegisterMBean( objectName );
		}
		JMXRegistrar.StatisticsInfo statisticsInfo = new JMXRegistrar.StatisticsInfo( statistics );
		JMXRegistrar.registerMBean( statisticsInfo, StatisticsInfoMBean.class, objectName );
		return objectName;
	}

	@Override
	public boolean isDirtyChecksEnabled() {
		return enableDirtyChecks;
	}

	@Override
	public boolean isStopped() {
		return stopped.get();
	}

	@Override
	public boolean isTransactionManagerExpected() {
		return this.transactionManagerExpected;
	}

	@Override
	public IndexManagerHolder getAllIndexesManager() {
		return getIndexManagerHolder();
	}

	@Override
	public IndexManagerHolder getIndexManagerHolder() {
		return this.allIndexesManager;
	}

	@Deprecated
	public EntityIndexBinding getSafeIndexBindingForEntity(Class<?> entityType) {
		if ( entityType == null ) {
			throw log.nullIsInvalidIndexedType();
		}
		EntityIndexBinding entityIndexBinding = getIndexBindings().get( entityType );
		if ( entityIndexBinding == null ) {
			throw log.notAnIndexedType( entityType.getName() );
		}
		return entityIndexBinding;
	}

	public EntityIndexBinding getSafeIndexBindingForEntity(IndexedTypeIdentifier type) {
		if ( type == null ) {
			throw log.nullIsInvalidIndexedType();
		}
		EntityIndexBinding entityIndexBinding = getIndexBinding( type );
		if ( entityIndexBinding == null ) {
			throw log.notAnIndexedType( type.getName() );
		}
		return entityIndexBinding;
	}

	@Override
	public ErrorHandler getErrorHandler() {
		return this.errorHandler;
	}

	@Override
	public IndexReaderAccessor getIndexReaderAccessor() {
		return indexReaderAccessor;
	}

	@Override
	public IndexedTypeDescriptor getIndexedTypeDescriptor(IndexedTypeIdentifier type) {
		IndexedTypeDescriptor typeDescriptor = indexedTypeDescriptors.get( type );
		if ( typeDescriptor == null ) {
			EntityIndexBinding indexBinder = indexBindingForEntities.get( type );
			IndexedTypeDescriptor indexedTypeDescriptor;
			if ( indexBinder == null ) {
				indexedTypeDescriptor = new IndexedTypeDescriptorForUnindexedType( type );
			}
			else {
				indexedTypeDescriptor = new IndexedTypeDescriptorImpl(
						indexBinder.getDocumentBuilder().getTypeMetadata(),
						indexBinder.getIndexManagerSelector().all()
				);
			}
			indexedTypeDescriptors.put( type, indexedTypeDescriptor );
			typeDescriptor = indexedTypeDescriptor;
		}
		return typeDescriptor;
	}

	@Override
	public IndexedTypeSet getIndexedTypeIdentifiers() {
		return indexBindingForEntities.keySet();
	}

	@Override
	public InstanceInitializer getInstanceInitializer() {
		return instanceInitializer;
	}

	@Override
	public TimeoutExceptionFactory getDefaultTimeoutExceptionFactory() {
		return timeoutExceptionFactory;
	}

	@Override
	public TimingSource getTimingSource() {
		return this.timingSource;
	}

	@Override
	public SearchMapping getProgrammaticMapping() {
		return mapping;
	}

	@Override
	public boolean isIndexMetadataComplete() {
		return this.indexMetadataIsComplete;
	}

	@Override
	public boolean isDeleteByTermEnforced() {
		return this.isDeleteByTermEnforced;
	}

	@Override
	public boolean isIdProvidedImplicit() {
		return isIdProvidedImplicit;
	}

	@Override
	public boolean isMultitenancyEnabled() {
		return isMultitenancyEnabled;
	}

	@Override
	public IndexManagerFactory getIndexManagerFactory() {
		return indexManagerFactory;
	}

	@Override
	public boolean enlistWorkerInTransaction() {
		return enlistWorkerInTransaction;
	}

	@Override
	public IndexManager getIndexManager(String indexName) {
		return getIndexManagerHolder().getIndexManager( indexName );
	}

	@Override
	public boolean isIndexUninvertingAllowed() {
		return indexUninvertingAllowed;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T unwrap(Class<T> cls) {
		if ( SearchIntegrator.class.isAssignableFrom( cls ) || ExtendedSearchIntegrator.class.isAssignableFrom( cls )
				|| SearchFactoryState.class.isAssignableFrom( cls ) ) {
			return (T) this;
		}
		else {
			throw new SearchException( "Can not unwrap an ImmutableSearchFactory into a '" + cls + "'" );
		}
	}

	@Override
	public LuceneWorkSerializer getWorkSerializer() {
		if ( workSerializer == null ) {
			workSerializer = serviceManager.requestService( LuceneWorkSerializer.class );
		}

		return workSerializer;
	}

	@Override
	public LuceneWorkSerializer getWorkSerializerState() {
		return workSerializer;
	}

	@Override
	public OperationDispatcher createRemoteOperationDispatcher(Predicate<IndexManager> predicate) {
		return new TransactionalOperationDispatcher( this, predicate );
	}

}
