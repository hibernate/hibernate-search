/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.engine.impl;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.search.backend.spi.BackendQueueProcessor;
import org.hibernate.search.backend.spi.Worker;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.cfg.SearchMapping;
import org.hibernate.search.cfg.spi.IndexManagerFactory;
import org.hibernate.search.engine.integration.impl.SearchIntegration;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.engine.spi.DocumentBuilderContainedEntity;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.engine.spi.TimingSource;
import org.hibernate.search.exception.ErrorHandler;
import org.hibernate.search.filter.FilterCachingStrategy;
import org.hibernate.search.indexes.impl.IndexManagerHolder;
import org.hibernate.search.indexes.serialization.spi.LuceneWorkSerializer;
import org.hibernate.search.indexes.spi.IndexManagerType;
import org.hibernate.search.query.engine.spi.TimeoutExceptionFactory;
import org.hibernate.search.spi.IndexingMode;
import org.hibernate.search.spi.InstanceInitializer;
import org.hibernate.search.spi.IndexedTypeMap;
import org.hibernate.search.spi.impl.ExtendedSearchIntegratorWithShareableState;
import org.hibernate.search.spi.impl.TypeHierarchy;
import org.hibernate.search.spi.impl.SearchFactoryState;
import org.hibernate.search.stat.Statistics;
import org.hibernate.search.util.configuration.impl.ConfigurationParseHelper;

/**
 * Shared factory state
 *
 * @author Emmanuel Bernard
 */
public class MutableSearchFactoryState implements SearchFactoryState {

	private IndexedTypeMap<DocumentBuilderContainedEntity> documentBuildersContainedEntities;
	private IndexedTypeMap<EntityIndexBinding> indexBindingsPerEntity;
	private IndexingMode indexingMode;
	private Worker worker;
	private BackendQueueProcessor backendQueueProcessor;
	private Map<String, FilterDef> filterDefinitions = new ConcurrentHashMap<>();
	private FilterCachingStrategy filterCachingStrategy;
	private Map<IndexManagerType, SearchIntegration> integrations = new ConcurrentHashMap<>();
	private int cacheBitResultsSize;
	private Properties configurationProperties;
	private TypeHierarchy configuredTypeHierarchy;
	private TypeHierarchy indexedTypeHierarchy;
	private ServiceManager serviceManager;
	private boolean transactionManagerExpected;
	private IndexManagerHolder allIndexesManager;
	private ErrorHandler errorHandler;
	private TimeoutExceptionFactory defaultTimeoutExceptionFactory;
	private InstanceInitializer instanceInitializer;
	private TimingSource timingSource;
	private SearchMapping mapping;
	private boolean indexMetadataIsComplete;
	private boolean deleteByTermEnforced;
	private boolean isIdProvidedImplicit;
	private boolean isMultitenancyEnabled;
	private IndexManagerFactory indexManagerFactory;
	private boolean enlistWorkerInTransaction;
	private Statistics statistics;
	private LuceneWorkSerializer workSerializer;

	public void copyStateFromOldFactory(SearchFactoryState oldFactoryState) {
		indexingMode = oldFactoryState.getIndexingMode();
		indexBindingsPerEntity = oldFactoryState.getIndexBindings();
		documentBuildersContainedEntities = oldFactoryState.getDocumentBuildersContainedEntities();
		worker = oldFactoryState.getWorker();
		filterDefinitions = oldFactoryState.getFilterDefinitions();
		filterCachingStrategy = oldFactoryState.getFilterCachingStrategy();
		integrations.clear();
		integrations.putAll( oldFactoryState.getIntegrations() );
		cacheBitResultsSize = oldFactoryState.getCacheBitResultsSize();
		configurationProperties = oldFactoryState.getConfigurationProperties();
		configuredTypeHierarchy = oldFactoryState.getConfiguredTypeHierarchy();
		indexedTypeHierarchy = oldFactoryState.getIndexedTypeHierarchy();
		serviceManager = oldFactoryState.getServiceManager();
		transactionManagerExpected = oldFactoryState.isTransactionManagerExpected();
		allIndexesManager = oldFactoryState.getAllIndexesManager();
		errorHandler = oldFactoryState.getErrorHandler();
		defaultTimeoutExceptionFactory = oldFactoryState.getDefaultTimeoutExceptionFactory();
		instanceInitializer = oldFactoryState.getInstanceInitializer();
		timingSource = oldFactoryState.getTimingSource();
		mapping = oldFactoryState.getProgrammaticMapping();
		indexMetadataIsComplete = oldFactoryState.isIndexMetadataComplete();
		deleteByTermEnforced = oldFactoryState.isDeleteByTermEnforced();
		isIdProvidedImplicit = oldFactoryState.isIdProvidedImplicit();
		isMultitenancyEnabled = oldFactoryState.isMultitenancyEnabled();
		indexManagerFactory = oldFactoryState.getIndexManagerFactory();
		enlistWorkerInTransaction = oldFactoryState.enlistWorkerInTransaction();
		statistics = oldFactoryState.getStatistics();
		workSerializer = oldFactoryState.getWorkSerializerState();
	}

	@Override
	public ServiceManager getServiceManager() {
		return serviceManager;
	}

	public void setServiceManager(ServiceManager serviceManager) {
		this.serviceManager = serviceManager;
	}

	@Override
	public IndexedTypeMap<DocumentBuilderContainedEntity> getDocumentBuildersContainedEntities() {
		return documentBuildersContainedEntities;
	}

	@Override
	public IndexedTypeMap<EntityIndexBinding> getIndexBindings() {
		return indexBindingsPerEntity;
	}

	@Override
	public IndexingMode getIndexingMode() {
		return indexingMode;
	}

	@Override
	public Worker getWorker() {
		return worker;
	}

	public BackendQueueProcessor getBackendQueueProcessor() {
		return backendQueueProcessor;
	}

	@Override
	public Map<String, FilterDef> getFilterDefinitions() {
		return filterDefinitions;
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
	public TypeHierarchy getConfiguredTypeHierarchy() {
		return configuredTypeHierarchy;
	}

	@Override
	public TypeHierarchy getIndexedTypeHierarchy() {
		return indexedTypeHierarchy;
	}

	public void setDocumentBuildersContainedEntities(IndexedTypeMap<DocumentBuilderContainedEntity> documentBuildersContainedEntities) {
		this.documentBuildersContainedEntities = documentBuildersContainedEntities;
	}

	public void setDocumentBuildersIndexedEntities(IndexedTypeMap<EntityIndexBinding> documentBuildersIndexedEntities) {
		this.indexBindingsPerEntity = documentBuildersIndexedEntities;
	}

	public void setIndexingMode(IndexingMode indexingMode) {
		this.indexingMode = indexingMode;
	}

	public void setWorker(Worker worker) {
		this.worker = worker;
	}

	public void setBackendQueueProcessor(BackendQueueProcessor backendQueueProcessor) {
		this.backendQueueProcessor = backendQueueProcessor;
	}

	public void addFilterDefinitions(Map<String, FilterDef> filterDefinitions) {
		this.filterDefinitions.putAll( filterDefinitions );
	}

	public void setFilterCachingStrategy(FilterCachingStrategy filterCachingStrategy) {
		this.filterCachingStrategy = filterCachingStrategy;
	}

	public void addIntegrations(Map<IndexManagerType, SearchIntegration> integrations) {
		this.integrations.putAll( integrations );
	}

	public void setCacheBitResultsSize(int cacheBitResultsSize) {
		this.cacheBitResultsSize = cacheBitResultsSize;
	}

	public void setConfigurationProperties(Properties configurationProperties) {
		this.configurationProperties = configurationProperties;
		this.enlistWorkerInTransaction = ConfigurationParseHelper.getBooleanValue(
				configurationProperties, Environment.WORKER_ENLIST_IN_TRANSACTION, false
		);
	}

	public void setConfiguredTypeHierarchy(TypeHierarchy configuredTypeHierarchy) {
		this.configuredTypeHierarchy = configuredTypeHierarchy;
	}

	public void setIndexedTypeHierarchy(TypeHierarchy indexedTypeHierarchy) {
		this.indexedTypeHierarchy = indexedTypeHierarchy;
	}

	@Override
	public boolean isTransactionManagerExpected() {
		return transactionManagerExpected;
	}

	public void setTransactionManagerExpected(boolean transactionManagerExpected) {
		this.transactionManagerExpected = transactionManagerExpected;
	}

	public void setAllIndexesManager(IndexManagerHolder indexesFactory) {
		this.allIndexesManager = indexesFactory;
	}

	@Override
	public IndexManagerHolder getAllIndexesManager() {
		return allIndexesManager;
	}

	public void setActiveSearchIntegrator(ExtendedSearchIntegratorWithShareableState factory) {
		allIndexesManager.setActiveSearchIntegrator( factory );
	}

	@Override
	public ErrorHandler getErrorHandler() {
		return this.errorHandler;
	}

	public void setErrorHandler(ErrorHandler errorHandler) {
		this.errorHandler = errorHandler;
	}

	@Override
	public InstanceInitializer getInstanceInitializer() {
		return instanceInitializer;
	}

	public void setInstanceInitializer(InstanceInitializer instanceInitializer) {
		this.instanceInitializer = instanceInitializer;
	}

	@Override
	public TimeoutExceptionFactory getDefaultTimeoutExceptionFactory() {
		return defaultTimeoutExceptionFactory;
	}

	public void setDefaultTimeoutExceptionFactory(TimeoutExceptionFactory defaultTimeoutExceptionFactory) {
		this.defaultTimeoutExceptionFactory = defaultTimeoutExceptionFactory;
	}

	@Override
	public TimingSource getTimingSource() {
		return this.timingSource;
	}

	public void setTimingSource(TimingSource timingSource) {
		this.timingSource = timingSource;
	}

	public void setProgrammaticMapping(SearchMapping mapping) {
		this.mapping = mapping;
	}

	@Override
	public SearchMapping getProgrammaticMapping() {
		return mapping;
	}

	public void setSearchMapping(SearchMapping mapping) {
		this.mapping = mapping;
	}

	@Override
	public boolean isIndexMetadataComplete() {
		return this.indexMetadataIsComplete;
	}

	@Override
	public boolean isDeleteByTermEnforced() {
		return this.deleteByTermEnforced;
	}

	public void setIndexMetadataComplete(boolean indexMetadataIsComplete) {
		this.indexMetadataIsComplete = indexMetadataIsComplete;
	}

	public void setDeleteByTermEnforced(boolean deleteByTermEnforced) {
		this.deleteByTermEnforced = deleteByTermEnforced;
	}

	@Override
	public boolean isIdProvidedImplicit() {
		return this.isIdProvidedImplicit;
	}

	public void setIdProvidedImplicit(boolean idProvidedImplicit) {
		this.isIdProvidedImplicit = idProvidedImplicit;
	}

	@Override
	public boolean isMultitenancyEnabled() {
		return isMultitenancyEnabled;
	}

	public void setMultitenancyEnabled(boolean isMultitenancyEnabled) {
		this.isMultitenancyEnabled = isMultitenancyEnabled;
	}

	@Override
	public IndexManagerFactory getIndexManagerFactory() {
		return indexManagerFactory;
	}

	@Override
	public boolean enlistWorkerInTransaction() {
		return enlistWorkerInTransaction;
	}

	public void setIndexManagerFactory(IndexManagerFactory indexManagerFactory) {
		this.indexManagerFactory = indexManagerFactory;
	}

	@Override
	public Statistics getStatistics() {
		return statistics;
	}

	public void setStatistics(Statistics statistics) {
		this.statistics = statistics;
	}

	/**
	 * Immutable: what's important here is not to forget that a
	 * workSerializer has already been requested, so that
	 * the factory that copies this state will be able to know
	 * it should release this service.
	 */
	@Override
	public LuceneWorkSerializer getWorkSerializerState() {
		return workSerializer;
	}

}
