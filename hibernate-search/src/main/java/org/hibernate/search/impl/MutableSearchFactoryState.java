/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */

package org.hibernate.search.impl;

import org.apache.lucene.analysis.Analyzer;
import org.hibernate.search.backend.BackendQueueProcessorFactory;
import org.hibernate.search.backend.LuceneIndexingParameters;
import org.hibernate.search.backend.Worker;
import org.hibernate.search.engine.DocumentBuilderContainedEntity;
import org.hibernate.search.engine.DocumentBuilderIndexedEntity;
import org.hibernate.search.engine.FilterDef;
import org.hibernate.search.engine.ServiceManager;
import org.hibernate.search.exception.ErrorHandler;
import org.hibernate.search.filter.FilterCachingStrategy;
import org.hibernate.search.reader.ReaderProvider;
import org.hibernate.search.spi.internals.DirectoryProviderData;
import org.hibernate.search.spi.internals.PolymorphicIndexHierarchy;
import org.hibernate.search.spi.internals.SearchFactoryState;
import org.hibernate.search.store.DirectoryProvider;

import java.util.Map;
import java.util.Properties;

/**
 * Shared factory state
 *
 * @author Emmanuel Bernard
 */
public class MutableSearchFactoryState implements SearchFactoryState {
	private Map<Class<?>, DocumentBuilderContainedEntity<?>> documentBuildersContainedEntities;
	private Map<DirectoryProvider<?>, DirectoryProviderData> directoryProviderData;
	private Map<Class<?>, DocumentBuilderIndexedEntity<?>> documentBuildersIndexedEntities;
	private String indexingStrategy;
	private Worker worker;
	private ReaderProvider readerProvider;
	private BackendQueueProcessorFactory backendQueueProcessorFactory;
	private Map<String, FilterDef> filterDefinitions;
	private FilterCachingStrategy filterCachingStrategy;
	private Map<String, Analyzer> analyzers;
	private int cacheBitResultsSize;
	private Properties configurationProperties;
	private ErrorHandler errorHandler;
	private PolymorphicIndexHierarchy indexHierarchy;
	private Map<DirectoryProvider, LuceneIndexingParameters> directoryProviderIndexingParams;
	private ServiceManager serviceManager;

	public void copyStateFromOldFactory(SearchFactoryState oldFactoryState) {
		indexingStrategy = oldFactoryState.getIndexingStrategy();
		documentBuildersIndexedEntities = oldFactoryState.getDocumentBuildersIndexedEntities();
		documentBuildersContainedEntities = oldFactoryState.getDocumentBuildersContainedEntities();
		directoryProviderData = oldFactoryState.getDirectoryProviderData();
		worker = oldFactoryState.getWorker();
		readerProvider = oldFactoryState.getReaderProvider();
		backendQueueProcessorFactory = oldFactoryState.getBackendQueueProcessorFactory();
		filterDefinitions = oldFactoryState.getFilterDefinitions();
		filterCachingStrategy = oldFactoryState.getFilterCachingStrategy();
		analyzers = oldFactoryState.getAnalyzers();
		cacheBitResultsSize = oldFactoryState.getCacheBitResultsSize();
		configurationProperties = oldFactoryState.getConfigurationProperties();
		errorHandler = oldFactoryState.getErrorHandler();
		indexHierarchy = oldFactoryState.getIndexHierarchy();
		directoryProviderIndexingParams = oldFactoryState.getDirectoryProviderIndexingParams();
		serviceManager = oldFactoryState.getServiceManager();
	}

	public ServiceManager getServiceManager() {
		return serviceManager;
	}

	public void setServiceManager(ServiceManager serviceManager) {
		this.serviceManager = serviceManager;
	}

	public Map<Class<?>, DocumentBuilderContainedEntity<?>> getDocumentBuildersContainedEntities() {
		return documentBuildersContainedEntities;
	}

	public Map<DirectoryProvider<?>, DirectoryProviderData> getDirectoryProviderData() {
		return directoryProviderData;
	}

	public Map<Class<?>, DocumentBuilderIndexedEntity<?>> getDocumentBuildersIndexedEntities() {
		return documentBuildersIndexedEntities;
	}

	public String getIndexingStrategy() {
		return indexingStrategy;
	}

	public Worker getWorker() {
		return worker;
	}

	public ReaderProvider getReaderProvider() {
		return readerProvider;
	}

	public BackendQueueProcessorFactory getBackendQueueProcessorFactory() {
		return backendQueueProcessorFactory;
	}

	public Map<String, FilterDef> getFilterDefinitions() {
		return filterDefinitions;
	}

	public FilterCachingStrategy getFilterCachingStrategy() {
		return filterCachingStrategy;
	}

	public Map<String, Analyzer> getAnalyzers() {
		return analyzers;
	}

	public int getCacheBitResultsSize() {
		return cacheBitResultsSize;
	}

	public Properties getConfigurationProperties() {
		return configurationProperties;
	}

	public ErrorHandler getErrorHandler() {
		return errorHandler;
	}

	public PolymorphicIndexHierarchy getIndexHierarchy() {
		return indexHierarchy;
	}

	public Map<DirectoryProvider, LuceneIndexingParameters> getDirectoryProviderIndexingParams() {
		return directoryProviderIndexingParams;
	}

	public void setDocumentBuildersContainedEntities(Map<Class<?>, DocumentBuilderContainedEntity<?>> documentBuildersContainedEntities) {
		this.documentBuildersContainedEntities = documentBuildersContainedEntities;
	}

	public void setDirectoryProviderData(Map<DirectoryProvider<?>, DirectoryProviderData> directoryProviderData) {
		this.directoryProviderData = directoryProviderData;
	}

	public void setDocumentBuildersIndexedEntities(Map<Class<?>, DocumentBuilderIndexedEntity<?>> documentBuildersIndexedEntities) {
		this.documentBuildersIndexedEntities = documentBuildersIndexedEntities;
	}

	public void setIndexingStrategy(String indexingStrategy) {
		this.indexingStrategy = indexingStrategy;
	}

	public void setWorker(Worker worker) {
		this.worker = worker;
	}

	public void setReaderProvider(ReaderProvider readerProvider) {
		this.readerProvider = readerProvider;
	}

	public void setBackendQueueProcessorFactory(BackendQueueProcessorFactory backendQueueProcessorFactory) {
		this.backendQueueProcessorFactory = backendQueueProcessorFactory;
	}

	public void setFilterDefinitions(Map<String, FilterDef> filterDefinitions) {
		this.filterDefinitions = filterDefinitions;
	}

	public void setFilterCachingStrategy(FilterCachingStrategy filterCachingStrategy) {
		this.filterCachingStrategy = filterCachingStrategy;
	}

	public void setAnalyzers(Map<String, Analyzer> analyzers) {
		this.analyzers = analyzers;
	}

	public void setCacheBitResultsSize(int cacheBitResultsSize) {
		this.cacheBitResultsSize = cacheBitResultsSize;
	}

	public void setConfigurationProperties(Properties configurationProperties) {
		this.configurationProperties = configurationProperties;
	}

	public void setErrorHandler(ErrorHandler errorHandler) {
		this.errorHandler = errorHandler;
	}

	public void setIndexHierarchy(PolymorphicIndexHierarchy indexHierarchy) {
		this.indexHierarchy = indexHierarchy;
	}

	public void setDirectoryProviderIndexingParams(Map<DirectoryProvider, LuceneIndexingParameters> directoryProviderIndexingParams) {
		this.directoryProviderIndexingParams = directoryProviderIndexingParams;
	}
}
