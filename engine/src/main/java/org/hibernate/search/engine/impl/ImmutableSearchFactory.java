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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.lucene.analysis.Analyzer;
import org.hibernate.search.backend.impl.batch.DefaultBatchBackend;
import org.hibernate.search.backend.spi.BatchBackend;
import org.hibernate.search.backend.spi.Worker;
import org.hibernate.search.batchindexing.MassIndexerProgressMonitor;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.cfg.SearchMapping;
import org.hibernate.search.cfg.spi.IndexManagerFactory;
import org.hibernate.search.engine.Version;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.engine.spi.AbstractDocumentBuilder;
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
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.jmx.StatisticsInfoMBean;
import org.hibernate.search.jmx.impl.JMXRegistrar;
import org.hibernate.search.metadata.IndexedTypeDescriptor;
import org.hibernate.search.metadata.impl.IndexedTypeDescriptorForUnindexedType;
import org.hibernate.search.metadata.impl.IndexedTypeDescriptorImpl;
import org.hibernate.search.query.DatabaseRetrievalMethod;
import org.hibernate.search.query.ObjectLookupMethod;
import org.hibernate.search.query.dsl.QueryContextBuilder;
import org.hibernate.search.query.dsl.impl.ConnectedQueryContextBuilder;
import org.hibernate.search.query.engine.impl.HSQueryImpl;
import org.hibernate.search.query.engine.spi.HSQuery;
import org.hibernate.search.query.engine.spi.TimeoutExceptionFactory;
import org.hibernate.search.spi.IndexingMode;
import org.hibernate.search.spi.InstanceInitializer;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.spi.WorkerBuildContext;
import org.hibernate.search.spi.impl.ExtendedSearchIntegratorWithShareableState;
import org.hibernate.search.spi.impl.PolymorphicIndexHierarchy;
import org.hibernate.search.spi.impl.SearchFactoryState;
import org.hibernate.search.stat.Statistics;
import org.hibernate.search.stat.impl.StatisticsImpl;
import org.hibernate.search.stat.spi.StatisticsImplementor;
import org.hibernate.search.util.configuration.impl.ConfigurationParseHelper;
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

	private final Map<Class<?>, EntityIndexBinding> indexBindingForEntities;
	private final Map<Class<?>, DocumentBuilderContainedEntity> documentBuildersContainedEntities;
	/**
	 * Lazily populated map of type descriptors
	 */
	private final ConcurrentHashMap<Class, IndexedTypeDescriptor> indexedTypeDescriptors;
	private final Worker worker;
	private final Map<String, FilterDef> filterDefinitions;
	private final FilterCachingStrategy filterCachingStrategy;
	private final Map<String, Analyzer> analyzers;
	private final AtomicBoolean stopped = new AtomicBoolean( false );
	private final int cacheBitResultsSize;
	private final Properties configurationProperties;
	private final PolymorphicIndexHierarchy indexHierarchy;
	private final StatisticsImpl statistics;
	private final boolean transactionManagerExpected;
	private final IndexManagerHolder allIndexesManager;
	private final ErrorHandler errorHandler;
	private final IndexingMode indexingMode;
	private final ServiceManager serviceManager;
	private final boolean enableDirtyChecks;
	private final DefaultIndexReaderAccessor indexReaderAccessor;
	private final InstanceInitializer instanceInitializer;
	private final TimeoutExceptionFactory timeoutExceptionFactory;
	private final TimingSource timingSource;
	private final SearchMapping mapping;
	private final boolean indexMetadataIsComplete;
	private final boolean isDeleteByTermEnforced;
	private final boolean isIdProvidedImplicit;
	private final String statisticsMBeanName;
	private final IndexManagerFactory indexManagerFactory;
	private final ObjectLookupMethod defaultObjectLookupMethod;
	private final DatabaseRetrievalMethod defaultDatabaseRetrievalMethod;
	private final boolean enlistWorkerInTransaction;
	private final boolean indexUninvertingAllowed;

	public ImmutableSearchFactory(SearchFactoryState state) {
		this.analyzers = state.getAnalyzers();
		this.cacheBitResultsSize = state.getCacheBitResultsSize();
		this.configurationProperties = state.getConfigurationProperties();
		this.indexBindingForEntities = state.getIndexBindings();
		this.documentBuildersContainedEntities = state.getDocumentBuildersContainedEntities();
		this.filterCachingStrategy = state.getFilterCachingStrategy();
		this.filterDefinitions = state.getFilterDefinitions();
		this.indexHierarchy = state.getIndexHierarchy();
		this.indexingMode = state.getIndexingMode();
		this.worker = state.getWorker();
		this.serviceManager = state.getServiceManager();
		this.transactionManagerExpected = state.isTransactionManagerExpected();
		this.allIndexesManager = state.getAllIndexesManager();
		this.errorHandler = state.getErrorHandler();
		this.instanceInitializer = state.getInstanceInitializer();
		this.timeoutExceptionFactory = state.getDefaultTimeoutExceptionFactory();
		this.timingSource = state.getTimingSource();
		this.mapping = state.getProgrammaticMapping();
		this.statistics = new StatisticsImpl( this );
		this.indexMetadataIsComplete = state.isIndexMetadataComplete();
		this.isDeleteByTermEnforced = state.isDeleteByTermEnforced();
		this.isIdProvidedImplicit = state.isIdProvidedImplicit();
		this.indexManagerFactory = state.getIndexManagerFactory();
		boolean statsEnabled = ConfigurationParseHelper.getBooleanValue(
				configurationProperties, Environment.GENERATE_STATS, false
		);
		this.statistics.setStatisticsEnabled( statsEnabled );

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
		this.indexedTypeDescriptors = new ConcurrentHashMap<>();

		this.defaultObjectLookupMethod = determineDefaultObjectLookupMethod();
		this.defaultDatabaseRetrievalMethod = determineDefaultDatabaseRetrievalMethod();
		this.enlistWorkerInTransaction = ConfigurationParseHelper.getBooleanValue(
				configurationProperties, Environment.WORKER_ENLIST_IN_TRANSACTION, false
		);

		this.indexUninvertingAllowed = ConfigurationParseHelper.getBooleanValue(
				configurationProperties, Environment.INDEX_UNINVERTING_ALLOWED, true
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
	@Deprecated
	public String getIndexingStrategy() {
		return indexingMode.toExternalRepresentation();
	}

	@Override
	public IndexingMode getIndexingMode() {
		return indexingMode;
	}

	@Override
	public void close() {
		if ( stopped.compareAndSet( false, true ) ) { //make sure we only stop once
			try {
				worker.close();
			}
			catch (Exception e) {
				log.workerException( e );
			}

			this.allIndexesManager.stop();
			this.timingSource.stop();

			serviceManager.releaseAllServices();

			for ( Analyzer an : this.analyzers.values() ) {
				an.close();
			}
			for ( AbstractDocumentBuilder documentBuilder : this.documentBuildersContainedEntities.values() ) {
				documentBuilder.close();
			}
			for ( EntityIndexBinding entityIndexBinding : this.indexBindingForEntities.values() ) {
				entityIndexBinding.getDocumentBuilder().close();
			}

			// unregister statistic mbean
			if ( statisticsMBeanName != null ) {
				JMXRegistrar.unRegisterMBean( statisticsMBeanName );
			}
		}
	}

	@Override
	public HSQuery createHSQuery() {
		return new HSQueryImpl( this );
	}

	@Override
	public Map<Class<?>, DocumentBuilderContainedEntity> getDocumentBuildersContainedEntities() {
		return documentBuildersContainedEntities;
	}

	@Override
	public Map<Class<?>, EntityIndexBinding> getIndexBindings() {
		return indexBindingForEntities;
	}

	@Override
	public EntityIndexBinding getIndexBinding(Class<?> entityType) {
		return indexBindingForEntities.get( entityType );
	}

	@SuppressWarnings("unchecked")
	@Override
	public DocumentBuilderContainedEntity getDocumentBuilderContainedEntity(Class entityType) {
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
	public void optimize(Class entityType) {
		EntityIndexBinding entityIndexBinding = getSafeIndexBindingForEntity( entityType );
		for ( IndexManager im : entityIndexBinding.getIndexManagers() ) {
			im.optimize();
		}
	}

	@Override
	public Analyzer getAnalyzer(String name) {
		final Analyzer analyzer = analyzers.get( name );
		if ( analyzer == null ) {
			throw new SearchException( "Unknown Analyzer definition: " + name );
		}
		return analyzer;
	}

	@Override
	public Analyzer getAnalyzer(Class<?> clazz) {
		EntityIndexBinding entityIndexBinding = getSafeIndexBindingForEntity( clazz );
		DocumentBuilderIndexedEntity builder = entityIndexBinding.getDocumentBuilder();
		return builder.getAnalyzer();
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
	public Map<String, Analyzer> getAnalyzers() {
		return analyzers;
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
	public Set<Class<?>> getIndexedTypesPolymorphic(Class<?>[] classes) {
		return indexHierarchy.getIndexedClasses( classes );
	}

	@Override
	public BatchBackend makeBatchBackend(MassIndexerProgressMonitor progressMonitor) {
		return new DefaultBatchBackend( this, progressMonitor );
	}

	@Override
	public PolymorphicIndexHierarchy getIndexHierarchy() {
		return indexHierarchy;
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

		// since the Searchintegrator is mutable we might have an already existing MBean which we have to unregister first
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

	public EntityIndexBinding getSafeIndexBindingForEntity(Class<?> entityType) {
		if ( entityType == null ) {
			throw log.nullIsInvalidIndexedType();
		}
		EntityIndexBinding entityIndexBinding = getIndexBinding( entityType );
		if ( entityIndexBinding == null ) {
			throw log.notAnIndexedType( entityType.getName() );
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
	public IndexedTypeDescriptor getIndexedTypeDescriptor(Class<?> entityType) {
		IndexedTypeDescriptor typeDescriptor;
		if ( indexedTypeDescriptors.containsKey( entityType ) ) {
			typeDescriptor = indexedTypeDescriptors.get( entityType );
		}
		else {
			EntityIndexBinding indexBinder = indexBindingForEntities.get( entityType );
			IndexedTypeDescriptor indexedTypeDescriptor;
			if ( indexBinder == null ) {
				indexedTypeDescriptor = new IndexedTypeDescriptorForUnindexedType( entityType );
			}
			else {
				indexedTypeDescriptor = new IndexedTypeDescriptorImpl(
						indexBinder.getDocumentBuilder().getMetadata(),
						indexBinder.getIndexManagers()
				);
			}
			indexedTypeDescriptors.put( entityType, indexedTypeDescriptor );
			typeDescriptor = indexedTypeDescriptor;
		}
		return typeDescriptor;
	}

	@Override
	public Set<Class<?>> getIndexedTypes() {
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

}
