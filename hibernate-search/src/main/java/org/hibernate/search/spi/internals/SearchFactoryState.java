package org.hibernate.search.spi.internals;

import org.apache.lucene.analysis.Analyzer;
import org.hibernate.search.backend.BackendQueueProcessorFactory;
import org.hibernate.search.backend.LuceneIndexingParameters;
import org.hibernate.search.backend.Worker;
import org.hibernate.search.engine.DocumentBuilderContainedEntity;
import org.hibernate.search.engine.DocumentBuilderIndexedEntity;
import org.hibernate.search.engine.FilterDef;
import org.hibernate.search.exception.ErrorHandler;
import org.hibernate.search.filter.FilterCachingStrategy;
import org.hibernate.search.reader.ReaderProvider;
import org.hibernate.search.store.DirectoryProvider;

import java.util.Map;
import java.util.Properties;

/**
 * Represents the sharable state of a search factory
 * 
 * @author Emmanuel Bernard
 */
public interface SearchFactoryState {
	Map<Class<?>, DocumentBuilderContainedEntity<?>> getDocumentBuildersContainedEntities();

	Map<DirectoryProvider<?>, DirectoryProviderData> getDirectoryProviderData();

	Map<Class<?>, DocumentBuilderIndexedEntity<?>> getDocumentBuildersIndexedEntities();

	String getIndexingStrategy();

	Worker getWorker();

	ReaderProvider getReaderProvider();

	BackendQueueProcessorFactory getBackendQueueProcessorFactory();

	void setBackendQueueProcessorFactory(BackendQueueProcessorFactory backendQueueProcessorFactory);

	Map<String, FilterDef> getFilterDefinitions();

	FilterCachingStrategy getFilterCachingStrategy();

	Map<String, Analyzer> getAnalyzers();

	int getCacheBitResultsSize();

	Properties getConfigurationProperties();

	ErrorHandler getErrorHandler();

	PolymorphicIndexHierarchy getIndexHierarchy();

	Map<DirectoryProvider, LuceneIndexingParameters> getDirectoryProviderIndexingParams();
}
