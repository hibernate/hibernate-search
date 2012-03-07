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

import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.lucene.analysis.Analyzer;
import org.hibernate.search.backend.impl.batch.BatchBackend;
import org.hibernate.search.backend.spi.Worker;
import org.hibernate.search.batchindexing.MassIndexerProgressMonitor;
import org.hibernate.search.cfg.SearchMapping;
import org.hibernate.search.engine.ServiceManager;
import org.hibernate.search.engine.impl.FilterDef;
import org.hibernate.search.engine.spi.DocumentBuilderContainedEntity;
import org.hibernate.search.engine.spi.EntityIndexBinder;
import org.hibernate.search.engine.spi.TimingSource;
import org.hibernate.search.exception.ErrorHandler;
import org.hibernate.search.filter.FilterCachingStrategy;
import org.hibernate.search.indexes.IndexReaderAccessor;
import org.hibernate.search.indexes.impl.IndexManagerHolder;
import org.hibernate.search.query.dsl.QueryContextBuilder;
import org.hibernate.search.query.engine.spi.HSQuery;
import org.hibernate.search.query.engine.spi.TimeoutExceptionFactory;
import org.hibernate.search.spi.InstanceInitializer;
import org.hibernate.search.spi.SearchFactoryBuilder;
import org.hibernate.search.spi.SearchFactoryIntegrator;
import org.hibernate.search.spi.internals.PolymorphicIndexHierarchy;
import org.hibernate.search.spi.internals.SearchFactoryImplementorWithShareableState;
import org.hibernate.search.stat.Statistics;
import org.hibernate.search.stat.spi.StatisticsImplementor;

/**
 * Factory delegating to a concrete implementation of another factory,
 * Useful to swap one factory for another.
 * Swapping factory is thread safe.
 *
 * @author Emmanuel Bernard
 */
public class MutableSearchFactory implements SearchFactoryImplementorWithShareableState, SearchFactoryIntegrator {
	//a reference to the same instance of this class is help by clients and various HSearch services
	//when changing the SearchFactory internals, only the underlying delegate should be changed.
	//the volatile ensure that the state is replicated upong underlying factory switch.
	private volatile SearchFactoryImplementorWithShareableState delegate;

	//lock to be acquired every time the underlying searchFactory is rebuilt
	private final Lock mutating = new ReentrantLock();

	public void setDelegate(SearchFactoryImplementorWithShareableState delegate) {
		this.delegate = delegate;
	}

	public Map<String, FilterDef> getFilterDefinitions() {
		return delegate.getFilterDefinitions();
	}

	public Map<Class<?>, EntityIndexBinder> getIndexBindingForEntity() {
		return delegate.getIndexBindingForEntity();
	}

	public EntityIndexBinder getIndexBindingForEntity(Class<?> entityType) {
		return delegate.getIndexBindingForEntity( entityType );
	}

	public <T> DocumentBuilderContainedEntity<T> getDocumentBuilderContainedEntity(Class<T> entityType) {
		return delegate.getDocumentBuilderContainedEntity( entityType );
	}

	public Worker getWorker() {
		return delegate.getWorker();
	}

	public FilterCachingStrategy getFilterCachingStrategy() {
		return delegate.getFilterCachingStrategy();
	}

	public Map<String, Analyzer> getAnalyzers() {
		return delegate.getAnalyzers();
	}

	public int getCacheBitResultsSize() {
		return delegate.getCacheBitResultsSize();
	}

	public Properties getConfigurationProperties() {
		return delegate.getConfigurationProperties();
	}

	public FilterDef getFilterDefinition(String name) {
		return delegate.getFilterDefinition( name );
	}

	public String getIndexingStrategy() {
		return delegate.getIndexingStrategy();
	}

	public void close() {
		delegate.close();
	}

	public HSQuery createHSQuery() {
		return delegate.createHSQuery();
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

	public boolean isJMXEnabled() {
		return delegate.isJMXEnabled();
	}

	public StatisticsImplementor getStatisticsImplementor() {
		return delegate.getStatisticsImplementor();
	}

	public PolymorphicIndexHierarchy getIndexHierarchy() {
		return delegate.getIndexHierarchy();
	}

	public ServiceManager getServiceManager() {
		return delegate.getServiceManager();
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

	public Statistics getStatistics() {
		return delegate.getStatistics();
	}

	public Map<Class<?>, DocumentBuilderContainedEntity<?>> getDocumentBuildersContainedEntities() {
		return delegate.getDocumentBuildersContainedEntities();
	}

	public void addClasses(Class<?>... classes) {
		//todo optimize the list of
		final SearchFactoryBuilder builder = new SearchFactoryBuilder().currentFactory( this );
		for ( Class<?> type : classes ) {
			builder.addClass( type );
		}
		try {
			mutating.lock();
			builder.buildSearchFactory();
		}
		finally {
			mutating.unlock();
		}
	}

	public boolean isDirtyChecksEnabled() {
		return delegate.isDirtyChecksEnabled();
	}

	public boolean isStopped() {
		return delegate.isStopped();
	}

	@Override
	public boolean isTransactionManagerExpected() {
		return delegate.isTransactionManagerExpected();
	}

	@Override
	public IndexManagerHolder getAllIndexesManager() {
		return delegate.getAllIndexesManager();
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

}
