package org.hibernate.search;

import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.lucene.search.Similarity;

import org.hibernate.search.backend.BackendQueueProcessorFactory;
import org.hibernate.search.backend.LuceneIndexingParameters;
import org.hibernate.search.exception.ErrorHandler;
import org.hibernate.search.store.DirectoryProvider;
import org.hibernate.search.store.optimization.OptimizerStrategy;

/**
 * Build context for the worker and other backend
 * Available after all index, entity metadata are built.
 *
 * @author Emmanuel Bernard
 */
public interface WorkerBuildContext extends BuildContext {
	void setBackendQueueProcessorFactory(BackendQueueProcessorFactory backendQueueProcessorFactory);
	public OptimizerStrategy getOptimizerStrategy(DirectoryProvider<?> provider);
	Set<Class<?>> getClassesInDirectoryProvider(DirectoryProvider<?> provider);
	LuceneIndexingParameters getIndexingParameters(DirectoryProvider<?> directoryProvider);
	ReentrantLock getDirectoryProviderLock(DirectoryProvider<?> provider);
	Similarity getSimilarity(DirectoryProvider<?> directoryProvider);
	boolean isExclusiveIndexUsageEnabled(DirectoryProvider<?> directoryProvider);
	ErrorHandler getErrorHandler();
}
