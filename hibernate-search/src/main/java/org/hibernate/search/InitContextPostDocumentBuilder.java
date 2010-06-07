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
 * @author Emmanuel Bernard
 */
public interface InitContextPostDocumentBuilder extends InitContext {
	void setBackendQueueProcessorFactory(BackendQueueProcessorFactory backendQueueProcessorFactory);
	public OptimizerStrategy getOptimizerStrategy(DirectoryProvider<?> provider);
	Set<Class<?>> getClassesInDirectoryProvider(DirectoryProvider<?> provider);
	LuceneIndexingParameters getIndexingParameters(DirectoryProvider<?> directoryProvider);
	ReentrantLock getDirectoryProviderLock(DirectoryProvider<?> provider);
	Similarity getSimilarity(DirectoryProvider<?> directoryProvider);
	boolean isExclusiveIndexUsageEnabled(DirectoryProvider<?> directoryProvider);
	ErrorHandler getErrorHandler();
}
