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
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.lucene.analysis.Analyzer;

import org.hibernate.search.backend.spi.BackendQueueProcessor;
import org.hibernate.search.backend.spi.Worker;
import org.hibernate.search.engine.impl.FilterDef;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.indexes.IndexReaderAccessor;
import org.hibernate.search.indexes.impl.DefaultIndexReaderAccessor;
import org.hibernate.search.indexes.impl.IndexManagerHolder;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.jmx.impl.JMXRegistrar;
import org.hibernate.search.stat.impl.StatisticsImpl;
import org.hibernate.search.stat.spi.StatisticsImplementor;
import org.hibernate.search.util.configuration.impl.ConfigurationParseHelper;
import org.hibernate.search.util.logging.impl.Log;

import org.hibernate.annotations.common.AssertionFailure;
import org.hibernate.search.Environment;
import org.hibernate.search.SearchException;
import org.hibernate.search.Version;
import org.hibernate.search.backend.impl.batch.BatchBackend;
import org.hibernate.search.backend.impl.batch.DefaultBatchBackend;
import org.hibernate.search.batchindexing.MassIndexerProgressMonitor;
import org.hibernate.search.cfg.SearchMapping;
import org.hibernate.search.engine.spi.AbstractDocumentBuilder;
import org.hibernate.search.engine.spi.DocumentBuilderContainedEntity;
import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;
import org.hibernate.search.engine.spi.EntityIndexBinder;
import org.hibernate.search.engine.spi.TimingSource;
import org.hibernate.search.engine.ServiceManager;
import org.hibernate.search.exception.ErrorHandler;
import org.hibernate.search.filter.FilterCachingStrategy;
import org.hibernate.search.jmx.StatisticsInfo;
import org.hibernate.search.jmx.StatisticsInfoMBean;
import org.hibernate.search.query.dsl.QueryContextBuilder;
import org.hibernate.search.query.dsl.impl.ConnectedQueryContextBuilder;
import org.hibernate.search.query.engine.spi.HSQuery;
import org.hibernate.search.query.engine.spi.TimeoutExceptionFactory;
import org.hibernate.search.query.engine.impl.HSQueryImpl;
import org.hibernate.search.spi.InstanceInitializer;
import org.hibernate.search.spi.ServiceProvider;
import org.hibernate.search.spi.WorkerBuildContext;
import org.hibernate.search.spi.internals.PolymorphicIndexHierarchy;
import org.hibernate.search.spi.internals.SearchFactoryImplementorWithShareableState;
import org.hibernate.search.spi.internals.SearchFactoryState;
import org.hibernate.search.stat.Statistics;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * This implementation is never directly exposed to the user, it is always wrapped into a {@link org.hibernate.search.impl.MutableSearchFactory}
 *
 * @author Emmanuel Bernard
 */
public class ImmutableSearchFactory implements SearchFactoryImplementorWithShareableState, WorkerBuildContext {

	static {
		Version.touch();
	}

	private static final Log log = LoggerFactory.make();

	private final Map<Class<?>, EntityIndexBinder> indexBindingForEntities;
	private final Map<Class<?>, DocumentBuilderContainedEntity<?>> documentBuildersContainedEntities;
	private final Worker worker;
	private final Map<String, FilterDef> filterDefinitions;
	private final FilterCachingStrategy filterCachingStrategy;
	private final Map<String, Analyzer> analyzers;
	private final AtomicBoolean stopped = new AtomicBoolean( false );
	private final int cacheBitResultsSize;
	private final Properties configurationProperties;
	private final PolymorphicIndexHierarchy indexHierarchy;
	private final StatisticsImpl statistics;
	private final boolean transactionManagerExpected;
	private final IndexManagerHolder allIndexesManager;
	private final ErrorHandler errorHandler;
	private final String indexingStrategy;
	private final ServiceManager serviceManager;
	private final boolean enableDirtyChecks;
	private final DefaultIndexReaderAccessor indexReaderAccessor;
	private final InstanceInitializer instanceInitializer;
	private final TimeoutExceptionFactory timeoutExceptionFactory;
	private final TimingSource timingSource;
	private final SearchMapping mapping;
	private final boolean indexMetadataIsComplete;

	public ImmutableSearchFactory(SearchFactoryState state) {
		this.analyzers = state.getAnalyzers();
		this.cacheBitResultsSize = state.getCacheBitResultsSize();
		this.configurationProperties = state.getConfigurationProperties();
		this.indexBindingForEntities = state.getIndexBindingForEntity();
		this.documentBuildersContainedEntities = state.getDocumentBuildersContainedEntities();
		this.filterCachingStrategy = state.getFilterCachingStrategy();
		this.filterDefinitions = state.getFilterDefinitions();
		this.indexHierarchy = state.getIndexHierarchy();
		this.indexingStrategy = state.getIndexingStrategy();
		this.worker = state.getWorker();
		this.serviceManager = state.getServiceManager();
		this.transactionManagerExpected = state.isTransactionManagerExpected();
		this.allIndexesManager = state.getAllIndexesManager();
		this.errorHandler = state.getErrorHandler();
		this.instanceInitializer = state.getInstanceInitializer();
		this.timeoutExceptionFactory = state.getDefaultTimeoutExceptionFactory();
		this.timingSource = state.getTimingSource();
		this.mapping = state.getProgrammaticMapping();
		this.statistics = new StatisticsImpl( this );
		this.indexMetadataIsComplete = state.isIndexMetadataComplete();
		boolean statsEnabled = ConfigurationParseHelper.getBooleanValue(
				configurationProperties, Environment.GENERATE_STATS, false
		);
		statistics.setStatisticsEnabled( statsEnabled );

		this.enableDirtyChecks = ConfigurationParseHelper.getBooleanValue(
				configurationProperties, Environment.ENABLE_DIRTY_CHECK, true
		);

		if ( isJMXEnabled() ) {
			// since the SearchFactory is mutable we might have an already existing MBean which we have to unregister first
			if ( JMXRegistrar.isNameRegistered( StatisticsInfoMBean.STATISTICS_MBEAN_OBJECT_NAME ) ) {
				JMXRegistrar.unRegisterMBean( StatisticsInfoMBean.STATISTICS_MBEAN_OBJECT_NAME );
			}
			JMXRegistrar.registerMBean(
					new StatisticsInfo( statistics ), StatisticsInfoMBean.STATISTICS_MBEAN_OBJECT_NAME
			);
		}

		this.indexReaderAccessor = new DefaultIndexReaderAccessor( this );
	}

	public Map<String, FilterDef> getFilterDefinitions() {
		return filterDefinitions;
	}

	public String getIndexingStrategy() {
		return indexingStrategy;
	}

	public void close() {
		if ( stopped.compareAndSet( false, true ) ) {  //make sure we only stop once
			try {
				worker.close();
			}
			catch ( Exception e ) {
				log.workerException( e );
			}

			this.allIndexesManager.stop();
			this.timingSource.stop();

			serviceManager.stopServices();

			for ( Analyzer an : this.analyzers.values() ) {
				an.close();
			}
			for ( AbstractDocumentBuilder<?> documentBuilder : this.documentBuildersContainedEntities.values() ) {
				documentBuilder.close();
			}
			for ( EntityIndexBinder entityBinder : this.indexBindingForEntities.values() ) {
				entityBinder.getDocumentBuilder().close();
			}
		}
	}

	public HSQuery createHSQuery() {
		return new HSQueryImpl( this );
	}

	public Map<Class<?>, DocumentBuilderContainedEntity<?>> getDocumentBuildersContainedEntities() {
		return documentBuildersContainedEntities;
	}

	public Map<Class<?>, EntityIndexBinder> getIndexBindingForEntity() {
		return indexBindingForEntities;
	}

	public EntityIndexBinder getIndexBindingForEntity(Class<?> entityType) {
		return indexBindingForEntities.get( entityType );
	}

	@SuppressWarnings("unchecked")
	public <T> DocumentBuilderContainedEntity<T> getDocumentBuilderContainedEntity(Class<T> entityType) {
		return (DocumentBuilderContainedEntity<T>) documentBuildersContainedEntities.get( entityType );
	}

	public void addClasses(Class<?>... classes) {
		throw new AssertionFailure( "Cannot add classes to an " + ImmutableSearchFactory.class.getName() );
	}

	public Worker getWorker() {
		return worker;
	}

	public void setBackendQueueProcessor(BackendQueueProcessor backendQueueProcessor) {
		throw new AssertionFailure( "ImmutableSearchFactory is immutable: should never be called" );
	}

	public void optimize() {
		for ( IndexManager im : this.allIndexesManager.getIndexManagers() ) {
			im.optimize();
		}
	}

	public void optimize(Class entityType) {
		EntityIndexBinder entityIndexBinding = getSafeIndexBindingForEntity( entityType );
		for ( IndexManager im: entityIndexBinding.getIndexManagers() ) {
			im.optimize();
		}
	}

	public Analyzer getAnalyzer(String name) {
		final Analyzer analyzer = analyzers.get( name );
		if ( analyzer == null ) {
			throw new SearchException( "Unknown Analyzer definition: " + name );
		}
		return analyzer;
	}

	public Analyzer getAnalyzer(Class<?> clazz) {
		EntityIndexBinder entityIndexBinding = getSafeIndexBindingForEntity( clazz );
		DocumentBuilderIndexedEntity<?> builder = entityIndexBinding.getDocumentBuilder();
		return builder.getAnalyzer();
	}

	public QueryContextBuilder buildQueryBuilder() {
		return new ConnectedQueryContextBuilder( this );
	}

	public Statistics getStatistics() {
		return statistics;
	}

	public StatisticsImplementor getStatisticsImplementor() {
		return statistics;
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

	public FilterDef getFilterDefinition(String name) {
		return filterDefinitions.get( name );
	}

	public <T> T requestService(Class<? extends ServiceProvider<T>> provider) {
		return serviceManager.requestService( provider, this );
	}

	public void releaseService(Class<? extends ServiceProvider<?>> provider) {
		serviceManager.releaseService( provider );
	}

	public int getFilterCacheBitResultsSize() {
		return cacheBitResultsSize;
	}

	public Set<Class<?>> getIndexedTypesPolymorphic(Class<?>[] classes) {
		return indexHierarchy.getIndexedClasses( classes );
	}

	public BatchBackend makeBatchBackend(MassIndexerProgressMonitor progressMonitor) {
		return new DefaultBatchBackend( this, progressMonitor );
	}

	public PolymorphicIndexHierarchy getIndexHierarchy() {
		return indexHierarchy;
	}

	public ServiceManager getServiceManager() {
		return serviceManager;
	}

	public SearchFactoryImplementor getUninitializedSearchFactory() {
		return this;
	}

	public boolean isJMXEnabled() {
		String enableJMX = getConfigurationProperties().getProperty( Environment.JMX_ENABLED );
		return "true".equalsIgnoreCase( enableJMX );
	}

	public boolean isDirtyChecksEnabled() {
		return enableDirtyChecks;
	}

	public boolean isStopped() {
		return stopped.get();
	}

	@Override
	public boolean isTransactionManagerExpected() {
		return this.transactionManagerExpected;
	}

	@Override
	public IndexManagerHolder getAllIndexesManager() {
		return this.allIndexesManager;
	}

	public EntityIndexBinder getSafeIndexBindingForEntity(Class<?> entityType) {
		if ( entityType == null ) {
			throw log.nullIsInvalidIndexedType();
		}
		EntityIndexBinder entityIndexBinding = getIndexBindingForEntity( entityType );
		if ( entityIndexBinding == null ) {
			throw log.notAnIndexedType( entityType.getName() );
		}
		return entityIndexBinding;
	}

	@Override
	public ErrorHandler getErrorHandler() {
		return this.errorHandler;
	}

	@Override
	public IndexReaderAccessor getIndexReaderAccessor() {
		return indexReaderAccessor;
	}

	@Override
	public InstanceInitializer getInstanceInitializer() {
		return instanceInitializer;
	}

	@Override
	public TimeoutExceptionFactory getDefaultTimeoutExceptionFactory() {
		return timeoutExceptionFactory;
	}

	@Override
	public TimingSource getTimingSource() {
		return this.timingSource;
	}

	@Override
	public SearchMapping getProgrammaticMapping() {
		return mapping;
	}

	@Override
	public boolean isIndexMetadataComplete() {
		return this.indexMetadataIsComplete;
	}

}
