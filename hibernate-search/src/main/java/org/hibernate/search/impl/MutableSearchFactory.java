package org.hibernate.search.impl;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.Similarity;

import org.hibernate.search.backend.BackendQueueProcessorFactory;
import org.hibernate.search.backend.LuceneIndexingParameters;
import org.hibernate.search.backend.Worker;
import org.hibernate.search.backend.impl.batchlucene.BatchBackend;
import org.hibernate.search.batchindexing.MassIndexerProgressMonitor;
import org.hibernate.search.engine.DocumentBuilderContainedEntity;
import org.hibernate.search.engine.DocumentBuilderIndexedEntity;
import org.hibernate.search.engine.FilterDef;
import org.hibernate.search.engine.SearchFactoryImplementor;
import org.hibernate.search.exception.ErrorHandler;
import org.hibernate.search.filter.FilterCachingStrategy;
import org.hibernate.search.query.dsl.v2.QueryContextBuilder;
import org.hibernate.search.reader.ReaderProvider;
import org.hibernate.search.store.DirectoryProvider;
import org.hibernate.search.store.optimization.OptimizerStrategy;

/**
 * Factory delegating to a concrete implementation of another factory,
 * Useful to swap one factory for another.
 * Swapping factory is thread safe.
 *
 * @author Emmanuel Bernard
 */
public class MutableSearchFactory implements SearchFactoryImplementor {
	private volatile SearchFactoryImplementor delegate;

	void setDelegate(SearchFactoryImplementor delegate) {
		this.delegate = delegate;
	}

	public BackendQueueProcessorFactory getBackendQueueProcessorFactory() {
		return delegate.getBackendQueueProcessorFactory();
	}

	public Map<Class<?>, DocumentBuilderIndexedEntity<?>> getDocumentBuildersIndexedEntities() {
		return delegate.getDocumentBuildersIndexedEntities();
	}

	public <T> DocumentBuilderIndexedEntity<T> getDocumentBuilderIndexedEntity(Class<T> entityType) {
		return delegate.getDocumentBuilderIndexedEntity( entityType );
	}

	public <T> DocumentBuilderContainedEntity<T> getDocumentBuilderContainedEntity(Class<T> entityType) {
		return delegate.getDocumentBuilderContainedEntity( entityType );
	}

	public Worker getWorker() {
		return delegate.getWorker();
	}

	public OptimizerStrategy getOptimizerStrategy(DirectoryProvider<?> provider) {
		return delegate.getOptimizerStrategy( provider );
	}

	public FilterCachingStrategy getFilterCachingStrategy() {
		return delegate.getFilterCachingStrategy();
	}

	public FilterDef getFilterDefinition(String name) {
		return delegate.getFilterDefinition( name );
	}

	public LuceneIndexingParameters getIndexingParameters(DirectoryProvider<?> provider) {
		return delegate.getIndexingParameters( provider );
	}

	public String getIndexingStrategy() {
		return delegate.getIndexingStrategy();
	}

	public void close() {
		delegate.close();
	}

	public Set<Class<?>> getClassesInDirectoryProvider(DirectoryProvider<?> directoryProvider) {
		return delegate.getClassesInDirectoryProvider( directoryProvider );
	}

	public Set<DirectoryProvider<?>> getDirectoryProviders() {
		return delegate.getDirectoryProviders();
	}

	public ReentrantLock getDirectoryProviderLock(DirectoryProvider<?> dp) {
		return delegate.getDirectoryProviderLock( dp );
	}

	public int getFilterCacheBitResultsSize() {
		return delegate.getFilterCacheBitResultsSize();
	}

	public Set<Class<?>> getIndexedTypesPolymorphic(Class<?>[] classes) {
		return delegate.getIndexedTypesPolymorphic( classes );
	}

	public BatchBackend makeBatchBackend(MassIndexerProgressMonitor progressMonitor) {
		return delegate.makeBatchBackend( progressMonitor );
	}

	public Similarity getSimilarity(DirectoryProvider<?> directoryProvider) {
		return delegate.getSimilarity( directoryProvider );
	}

	public boolean isExclusiveIndexUsageEnabled(DirectoryProvider<?> provider) {
		return delegate.isExclusiveIndexUsageEnabled( provider );
	}

	public ErrorHandler getErrorHandler() {
		return delegate.getErrorHandler();
	}

	public ReaderProvider getReaderProvider() {
		return delegate.getReaderProvider();
	}

	public DirectoryProvider[] getDirectoryProviders(Class<?> entity) {
		return delegate.getDirectoryProviders( entity );
	}

	public void optimize() {
		delegate.optimize();
	}

	public void optimize(Class entityType) {
		delegate.optimize( entityType );
	}

	public Analyzer getAnalyzer(String name) {
		return delegate.getAnalyzer( name );
	}

	public Analyzer getAnalyzer(Class<?> clazz) {
		return delegate.getAnalyzer( clazz );
	}

	public QueryContextBuilder buildQueryBuilder() {
		return delegate.buildQueryBuilder();
	}
}
