// $Id$
package org.hibernate.search.engine;

import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import org.hibernate.search.SearchFactory;
import org.hibernate.search.backend.BackendQueueProcessorFactory;
import org.hibernate.search.backend.LuceneIndexingParameters;
import org.hibernate.search.backend.Worker;
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

	Map<Class, DocumentBuilder<Object>> getDocumentBuilders();

	Map<DirectoryProvider, ReentrantLock> getLockableDirectoryProviders();

	Worker getWorker();

	void addOptimizerStrategy(DirectoryProvider<?> provider, OptimizerStrategy optimizerStrategy);

	OptimizerStrategy getOptimizerStrategy(DirectoryProvider<?> provider);

	FilterCachingStrategy getFilterCachingStrategy();

	FilterDef getFilterDefinition(String name);

	public LuceneIndexingParameters getIndexingParameters(DirectoryProvider<?> provider);

	void addIndexingParameters(DirectoryProvider<?> provider, LuceneIndexingParameters indexingParams);

	public String getIndexingStrategy();

	public void close();
}
