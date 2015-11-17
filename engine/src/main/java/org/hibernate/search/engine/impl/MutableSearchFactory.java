/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.impl;

import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.lucene.analysis.Analyzer;
import org.hibernate.search.backend.spi.BatchBackend;
import org.hibernate.search.backend.spi.Worker;
import org.hibernate.search.batchindexing.MassIndexerProgressMonitor;
import org.hibernate.search.cfg.SearchMapping;
import org.hibernate.search.cfg.spi.IndexManagerFactory;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.engine.spi.DocumentBuilderContainedEntity;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.engine.spi.TimingSource;
import org.hibernate.search.exception.ErrorHandler;
import org.hibernate.search.filter.FilterCachingStrategy;
import org.hibernate.search.indexes.IndexReaderAccessor;
import org.hibernate.search.indexes.impl.IndexManagerHolder;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.metadata.IndexedTypeDescriptor;
import org.hibernate.search.query.DatabaseRetrievalMethod;
import org.hibernate.search.query.ObjectLookupMethod;
import org.hibernate.search.query.dsl.QueryContextBuilder;
import org.hibernate.search.query.engine.spi.HSQuery;
import org.hibernate.search.query.engine.spi.TimeoutExceptionFactory;
import org.hibernate.search.spi.IndexingMode;
import org.hibernate.search.spi.InstanceInitializer;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.spi.SearchIntegratorBuilder;
import org.hibernate.search.spi.WorkerBuildContext;
import org.hibernate.search.spi.impl.ExtendedSearchIntegratorWithShareableState;
import org.hibernate.search.spi.impl.PolymorphicIndexHierarchy;
import org.hibernate.search.stat.Statistics;
import org.hibernate.search.stat.spi.StatisticsImplementor;

/**
 * Factory delegating to a concrete implementation of another factory. Useful to swap one factory for another.
 *
 * Swapping factory is thread safe.
 *
 * @author Emmanuel Bernard
 */
public class MutableSearchFactory implements ExtendedSearchIntegratorWithShareableState, SearchIntegrator, WorkerBuildContext {
	// Implements WorkerBuilderContext for the dynamic sharding approach which build IndexManager lazily

	//a reference to the same instance of this class is help by clients and various HSearch services
	//when changing the SearchFactory internals, only the underlying delegate should be changed.
	//the volatile ensure that the state is replicated upon underlying factory switch.
	private volatile ExtendedSearchIntegratorWithShareableState delegate;

	//lock to be acquired every time the underlying searchFactory is rebuilt
	private final Lock mutating = new ReentrantLock();

	public void setDelegate(ExtendedSearchIntegratorWithShareableState delegate) {
		this.delegate = delegate;
	}

	@Override
	public Map<String, FilterDef> getFilterDefinitions() {
		return delegate.getFilterDefinitions();
	}

	@Override
	public Map<Class<?>, EntityIndexBinding> getIndexBindings() {
		return delegate.getIndexBindings();
	}

	@Override
	public EntityIndexBinding getIndexBinding(Class<?> entityType) {
		return delegate.getIndexBinding( entityType );
	}

	@Override
	public DocumentBuilderContainedEntity getDocumentBuilderContainedEntity(Class<?> entityType) {
		return delegate.getDocumentBuilderContainedEntity( entityType );
	}

	@Override
	public Worker getWorker() {
		return delegate.getWorker();
	}

	@Override
	public FilterCachingStrategy getFilterCachingStrategy() {
		return delegate.getFilterCachingStrategy();
	}

	@Override
	public Map<String, Analyzer> getAnalyzers() {
		return delegate.getAnalyzers();
	}

	@Override
	public int getCacheBitResultsSize() {
		return delegate.getCacheBitResultsSize();
	}

	@Override
	public Properties getConfigurationProperties() {
		return delegate.getConfigurationProperties();
	}

	@Override
	public FilterDef getFilterDefinition(String name) {
		return delegate.getFilterDefinition( name );
	}

	@Override
	public ExtendedSearchIntegrator getUninitializedSearchIntegrator() {
		return this;
	}

	@Override
	@Deprecated
	public String getIndexingStrategy() {
		return delegate.getIndexingMode().toExternalRepresentation();
	}

	@Override
	public IndexingMode getIndexingMode() {
		return delegate.getIndexingMode();
	}

	@Override
	public void close() {
		delegate.close();
	}

	@Override
	public HSQuery createHSQuery() {
		return delegate.createHSQuery();
	}

	@Override
	public int getFilterCacheBitResultsSize() {
		return delegate.getFilterCacheBitResultsSize();
	}

	@Override
	public Set<Class<?>> getIndexedTypesPolymorphic(Class<?>[] classes) {
		return delegate.getIndexedTypesPolymorphic( classes );
	}

	@Override
	public BatchBackend makeBatchBackend(MassIndexerProgressMonitor progressMonitor) {
		return delegate.makeBatchBackend( progressMonitor );
	}

	@Override
	public boolean isJMXEnabled() {
		return delegate.isJMXEnabled();
	}

	@Override
	public StatisticsImplementor getStatisticsImplementor() {
		return delegate.getStatisticsImplementor();
	}

	@Override
	public PolymorphicIndexHierarchy getIndexHierarchy() {
		return delegate.getIndexHierarchy();
	}

	@Override
	public ServiceManager getServiceManager() {
		return delegate.getServiceManager();
	}

	@Override
	public DatabaseRetrievalMethod getDefaultDatabaseRetrievalMethod() {
		return delegate.getDefaultDatabaseRetrievalMethod();
	}

	@Override
	public ObjectLookupMethod getDefaultObjectLookupMethod() {
		return delegate.getDefaultObjectLookupMethod();
	}

	@Override
	public void optimize() {
		delegate.optimize();
	}

	@Override
	public void optimize(Class entityType) {
		delegate.optimize( entityType );
	}

	@Override
	public Analyzer getAnalyzer(String name) {
		return delegate.getAnalyzer( name );
	}

	@Override
	public Analyzer getAnalyzer(Class<?> clazz) {
		return delegate.getAnalyzer( clazz );
	}

	@Override
	public QueryContextBuilder buildQueryBuilder() {
		return delegate.buildQueryBuilder();
	}

	@Override
	public Statistics getStatistics() {
		return delegate.getStatistics();
	}

	@Override
	public Map<Class<?>, DocumentBuilderContainedEntity> getDocumentBuildersContainedEntities() {
		return delegate.getDocumentBuildersContainedEntities();
	}

	@Override
	public void addClasses(Class<?>... classes) {
		final SearchIntegratorBuilder builder = new SearchIntegratorBuilder().currentSearchIntegrator( this );
		for ( Class<?> type : classes ) {
			builder.addClass( type );
		}
		try {
			mutating.lock();
			builder.buildSearchIntegrator();
		}
		finally {
			mutating.unlock();
		}
	}

	@Override
	public boolean isDirtyChecksEnabled() {
		return delegate.isDirtyChecksEnabled();
	}

	@Override
	public boolean isStopped() {
		return delegate.isStopped();
	}

	@Override
	public boolean isTransactionManagerExpected() {
		return delegate.isTransactionManagerExpected();
	}

	@Override
	public IndexManagerHolder getAllIndexesManager() {
		return getIndexManagerHolder();
	}

	@Override
	public IndexManagerHolder getIndexManagerHolder() {
		return delegate.getIndexManagerHolder();
	}

	@Override
	public ErrorHandler getErrorHandler() {
		return delegate.getErrorHandler();
	}

	@Override
	public IndexReaderAccessor getIndexReaderAccessor() {
		return delegate.getIndexReaderAccessor();
	}

	@Override
	public IndexedTypeDescriptor getIndexedTypeDescriptor(Class<?> entityType) {
		return delegate.getIndexedTypeDescriptor( entityType );
	}

	@Override
	public Set<Class<?>> getIndexedTypes() {
		return delegate.getIndexedTypes();
	}

	@Override
	public InstanceInitializer getInstanceInitializer() {
		return delegate.getInstanceInitializer();
	}

	@Override
	public TimeoutExceptionFactory getDefaultTimeoutExceptionFactory() {
		return delegate.getDefaultTimeoutExceptionFactory();
	}

	@Override
	public TimingSource getTimingSource() {
		return delegate.getTimingSource();
	}

	@Override
	public SearchMapping getProgrammaticMapping() {
		return delegate.getProgrammaticMapping();
	}

	@Override
	public boolean isIndexMetadataComplete() {
		return delegate.isIndexMetadataComplete();
	}

	@Override
	public boolean isDeleteByTermEnforced() {
		return delegate.isDeleteByTermEnforced();
	}

	@Override
	public boolean isIdProvidedImplicit() {
		return delegate.isIdProvidedImplicit();
	}

	@Override
	public IndexManagerFactory getIndexManagerFactory() {
		return delegate.getIndexManagerFactory();
	}

	@Override
	public boolean enlistWorkerInTransaction() {
		return delegate.enlistWorkerInTransaction();
	}

	@Override
	public IndexManager getIndexManager(String indexName) {
		return delegate.getIndexManager( indexName );
	}

	@Override
	public boolean isIndexUninvertingAllowed() {
		return delegate.isIndexUninvertingAllowed();
	}

	@Override
	public <T> T unwrap(Class<T> cls) {
		if ( SearchIntegrator.class.equals( cls ) || ExtendedSearchIntegrator.class.equals( cls ) || MutableSearchFactory.class.equals( cls ) ) {
			return (T) this;
		}
		else {
			return delegate.unwrap( cls );
		}
	}

}
