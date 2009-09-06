// $Id$
package org.hibernate.search.engine;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import org.hibernate.search.SearchFactory;
import org.hibernate.search.backend.BackendQueueProcessorFactory;
import org.hibernate.search.backend.LuceneIndexingParameters;
import org.hibernate.search.backend.Worker;
import org.hibernate.search.backend.impl.batchlucene.BatchBackend;
import org.hibernate.search.batchindexing.MassIndexerProgressMonitor;
import org.hibernate.search.filter.FilterCachingStrategy;
import org.hibernate.search.store.DirectoryProvider;
import org.hibernate.search.store.optimization.OptimizerStrategy;

/**
 * Interface which gives access to the different directory providers and their configuration.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public interface SearchFactoryImplementor extends SearchFactory {
	BackendQueueProcessorFactory getBackendQueueProcessorFactory();

	void setBackendQueueProcessorFactory(BackendQueueProcessorFactory backendQueueProcessorFactory);

	Map<Class<?>, DocumentBuilderIndexedEntity<?>> getDocumentBuildersIndexedEntities();

	<T> DocumentBuilderIndexedEntity<T> getDocumentBuilderIndexedEntity(Class<T> entityType);

	<T> DocumentBuilderContainedEntity<T> getDocumentBuilderContainedEntity(Class<T> entityType);

	Worker getWorker();

	void addOptimizerStrategy(DirectoryProvider<?> provider, OptimizerStrategy optimizerStrategy);

	OptimizerStrategy getOptimizerStrategy(DirectoryProvider<?> provider);

	FilterCachingStrategy getFilterCachingStrategy();

	FilterDef getFilterDefinition(String name);

	LuceneIndexingParameters getIndexingParameters(DirectoryProvider<?> provider);

	void addIndexingParameters(DirectoryProvider<?> provider, LuceneIndexingParameters indexingParams);

	String getIndexingStrategy();

	void close();

	void addClassToDirectoryProvider(Class<?> clazz, DirectoryProvider<?> directoryProvider);

	Set<Class<?>> getClassesInDirectoryProvider(DirectoryProvider<?> directoryProvider);

	Set<DirectoryProvider<?>> getDirectoryProviders();

	ReentrantLock getDirectoryProviderLock(DirectoryProvider<?> dp);

	void addDirectoryProvider(DirectoryProvider<?> provider);
	
	int getFilterCacheBitResultsSize();

	Set<Class<?>> getIndexedTypesPolymorphic(Class<?>[] classes);
	
	BatchBackend makeBatchBackend(MassIndexerProgressMonitor progressMonitor);
}
