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

import org.apache.lucene.analysis.Analyzer;
import org.hibernate.search.backend.spi.BackendQueueProcessor;
import org.hibernate.search.backend.spi.Worker;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.cfg.SearchMapping;
import org.hibernate.search.cfg.spi.IndexManagerFactory;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.engine.spi.DocumentBuilderContainedEntity;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.engine.spi.TimingSource;
import org.hibernate.search.exception.ErrorHandler;
import org.hibernate.search.filter.FilterCachingStrategy;
import org.hibernate.search.indexes.impl.IndexManagerHolder;
import org.hibernate.search.query.engine.spi.TimeoutExceptionFactory;
import org.hibernate.search.spi.IndexingMode;
import org.hibernate.search.spi.InstanceInitializer;
import org.hibernate.search.spi.impl.ExtendedSearchIntegratorWithShareableState;
import org.hibernate.search.spi.impl.PolymorphicIndexHierarchy;
import org.hibernate.search.spi.impl.SearchFactoryState;
import org.hibernate.search.util.configuration.impl.ConfigurationParseHelper;

/**
 * Shared factory state
 *
 * @author Emmanuel Bernard
 */
public class MutableSearchFactoryState implements SearchFactoryState {

	private Map<Class<?>, DocumentBuilderContainedEntity> documentBuildersContainedEntities;
	private Map<Class<?>, EntityIndexBinding> indexBindingsPerEntity;
	private IndexingMode indexingMode;
	private Worker worker;
	private BackendQueueProcessor backendQueueProcessor;
	private Map<String, FilterDef> filterDefinitions = new ConcurrentHashMap<>();
	private FilterCachingStrategy filterCachingStrategy;
	private Map<String, Analyzer> analyzers = new ConcurrentHashMap<>();
	private int cacheBitResultsSize;
	private Properties configurationProperties;
	private PolymorphicIndexHierarchy indexHierarchy;
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
	private IndexManagerFactory indexManagerFactory;
	private boolean enlistWorkerInTransaction;

	public void copyStateFromOldFactory(SearchFactoryState oldFactoryState) {
		indexingMode = oldFactoryState.getIndexingMode();
		indexBindingsPerEntity = oldFactoryState.getIndexBindings();
		documentBuildersContainedEntities = oldFactoryState.getDocumentBuildersContainedEntities();
		worker = oldFactoryState.getWorker();
		filterDefinitions = oldFactoryState.getFilterDefinitions();
		filterCachingStrategy = oldFactoryState.getFilterCachingStrategy();
		analyzers = oldFactoryState.getAnalyzers();
		cacheBitResultsSize = oldFactoryState.getCacheBitResultsSize();
		configurationProperties = oldFactoryState.getConfigurationProperties();
		indexHierarchy = oldFactoryState.getIndexHierarchy();
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
		indexManagerFactory = oldFactoryState.getIndexManagerFactory();
		enlistWorkerInTransaction = oldFactoryState.enlistWorkerInTransaction();
	}

	@Override
	public ServiceManager getServiceManager() {
		return serviceManager;
	}

	public void setServiceManager(ServiceManager serviceManager) {
		this.serviceManager = serviceManager;
	}

	@Override
	public Map<Class<?>, DocumentBuilderContainedEntity> getDocumentBuildersContainedEntities() {
		return documentBuildersContainedEntities;
	}

	@Override
	public Map<Class<?>, EntityIndexBinding> getIndexBindings() {
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
	public PolymorphicIndexHierarchy getIndexHierarchy() {
		return indexHierarchy;
	}

	public void setDocumentBuildersContainedEntities(Map<Class<?>, DocumentBuilderContainedEntity> documentBuildersContainedEntities) {
		this.documentBuildersContainedEntities = documentBuildersContainedEntities;
	}

	public void setDocumentBuildersIndexedEntities(Map<Class<?>, EntityIndexBinding> documentBuildersIndexedEntities) {
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

	public void addAnalyzers(Map<String, Analyzer> analyzers) {
		this.analyzers.putAll( analyzers );
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

	public void setIndexHierarchy(PolymorphicIndexHierarchy indexHierarchy) {
		this.indexHierarchy = indexHierarchy;
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

}
