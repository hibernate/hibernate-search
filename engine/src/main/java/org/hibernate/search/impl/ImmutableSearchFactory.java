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

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.lucene.analysis.Analyzer;
import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.Environment;
import org.hibernate.search.SearchException;
import org.hibernate.search.Version;
import org.hibernate.search.backend.impl.batch.BatchBackend;
import org.hibernate.search.backend.impl.batch.DefaultBatchBackend;
import org.hibernate.search.backend.spi.Worker;
import org.hibernate.search.batchindexing.MassIndexerProgressMonitor;
import org.hibernate.search.cfg.SearchMapping;
import org.hibernate.search.cfg.spi.IndexManagerFactory;
import org.hibernate.search.engine.ServiceManager;
import org.hibernate.search.engine.impl.FilterDef;
import org.hibernate.search.engine.spi.AbstractDocumentBuilder;
import org.hibernate.search.engine.spi.DocumentBuilderContainedEntity;
import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;
import org.hibernate.search.engine.spi.EntityIndexBinder;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.engine.spi.TimingSource;
import org.hibernate.search.exception.ErrorHandler;
import org.hibernate.search.filter.FilterCachingStrategy;
import org.hibernate.search.indexes.IndexReaderAccessor;
import org.hibernate.search.indexes.impl.DefaultIndexReaderAccessor;
import org.hibernate.search.indexes.impl.IndexManagerHolder;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.jmx.StatisticsInfo;
import org.hibernate.search.jmx.StatisticsInfoMBean;
import org.hibernate.search.jmx.impl.JMXRegistrar;
import org.hibernate.search.metadata.IndexedTypeDescriptor;
import org.hibernate.search.metadata.impl.IndexedTypeDescriptorForUnindexedType;
import org.hibernate.search.metadata.impl.IndexedTypeDescriptorImpl;
import org.hibernate.search.query.dsl.QueryContextBuilder;
import org.hibernate.search.query.dsl.impl.ConnectedQueryContextBuilder;
import org.hibernate.search.query.engine.impl.HSQueryImpl;
import org.hibernate.search.query.engine.spi.HSQuery;
import org.hibernate.search.query.engine.spi.TimeoutExceptionFactory;
import org.hibernate.search.spi.InstanceInitializer;
import org.hibernate.search.spi.ServiceProvider;
import org.hibernate.search.spi.WorkerBuildContext;
import org.hibernate.search.spi.internals.PolymorphicIndexHierarchy;
import org.hibernate.search.spi.internals.SearchFactoryImplementorWithShareableState;
import org.hibernate.search.spi.internals.SearchFactoryState;
import org.hibernate.search.stat.Statistics;
import org.hibernate.search.stat.impl.StatisticsImpl;
import org.hibernate.search.stat.spi.StatisticsImplementor;
import org.hibernate.search.util.configuration.impl.ConfigurationParseHelper;
import org.hibernate.search.util.logging.impl.Log;
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

	private final Map<Class<?>, EntityIndexBinding> indexBindingForEntities;
	private final Map<Class<?>, DocumentBuilderContainedEntity<?>> documentBuildersContainedEntities;
	/**
	 * Lazily populated map of type descriptors
	 */
	private final ConcurrentHashMap<Class<?>, IndexedTypeDescriptor> indexedTypeDescriptors;
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
	private final boolean isIdProvidedImplicit;
	private final String statisticsMBeanName;
	private final IndexManagerFactory indexManagerFactory;

	public ImmutableSearchFactory(SearchFactoryState state) {
		this.analyzers = state.getAnalyzers();
		this.cacheBitResultsSize = state.getCacheBitResultsSize();
		this.configurationProperties = state.getConfigurationProperties();
		this.indexBindingForEntities = state.getIndexBindings();
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
		this.isIdProvidedImplicit = state.isIdProvidedImplicit();
		this.indexManagerFactory = state.getIndexManagerFactory();
		boolean statsEnabled = ConfigurationParseHelper.getBooleanValue(
				configurationProperties, Environment.GENERATE_STATS, false
		);
		this.statistics.setStatisticsEnabled( statsEnabled );

		this.enableDirtyChecks = ConfigurationParseHelper.getBooleanValue(
				configurationProperties, Environment.ENABLE_DIRTY_CHECK, true
		);

		if ( isJMXEnabled() ) {
			this.statisticsMBeanName = registerMBeans();
		}
		else {
			this.statisticsMBeanName = null;
		}

		this.indexReaderAccessor = new DefaultIndexReaderAccessor( this );
		this.indexedTypeDescriptors = new ConcurrentHashMap<Class<?>, IndexedTypeDescriptor>();
	}

	@Override
	public Map<String, FilterDef> getFilterDefinitions() {
		return filterDefinitions;
	}

	@Override
	public String getIndexingStrategy() {
		return indexingStrategy;
	}

	@Override
	public void close() {
		if ( stopped.compareAndSet( false, true ) ) { //make sure we only stop once
			try {
				worker.close();
			}
			catch (Exception e) {
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
			for ( EntityIndexBinding entityIndexBinding : this.indexBindingForEntities.values() ) {
				entityIndexBinding.getDocumentBuilder().close();
			}

			// unregister statistic mbean
			if ( statisticsMBeanName != null ) {
				JMXRegistrar.unRegisterMBean( statisticsMBeanName );
			}
		}
	}

	@Override
	public HSQuery createHSQuery() {
		return new HSQueryImpl( this );
	}

	@Override
	public Map<Class<?>, DocumentBuilderContainedEntity<?>> getDocumentBuildersContainedEntities() {
		return documentBuildersContainedEntities;
	}

	@Override
	public Map<Class<?>, EntityIndexBinding> getIndexBindings() {
		return indexBindingForEntities;
	}

	@Override
	public Map<Class<?>, EntityIndexBinder> getIndexBindingForEntity() {
		Map<Class<?>, EntityIndexBinder> tmpMap = new HashMap<Class<?>, EntityIndexBinder>();
		for ( Map.Entry<Class<?>, EntityIndexBinding> entry : indexBindingForEntities.entrySet() ) {
			tmpMap.put( entry.getKey(), new EntityIndexBindingWrapper( entry.getValue() ) );
		}
		return tmpMap;
	}

	@Override
	public EntityIndexBinder getIndexBindingForEntity(Class<?> entityType) {
		final EntityIndexBinding entityIndexBinding = indexBindingForEntities.get( entityType );
		if ( entityIndexBinding == null ) {
			return null;
		}
		else {
			return new EntityIndexBindingWrapper( entityIndexBinding );
		}
	}

	@Override
	public EntityIndexBinding getIndexBinding(Class<?> entityType) {
		return indexBindingForEntities.get( entityType );
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> DocumentBuilderContainedEntity<T> getDocumentBuilderContainedEntity(Class<T> entityType) {
		return (DocumentBuilderContainedEntity<T>) documentBuildersContainedEntities.get( entityType );
	}

	@Override
	public void addClasses(Class<?>... classes) {
		throw new AssertionFailure( "Cannot add classes to an " + ImmutableSearchFactory.class.getName() );
	}

	@Override
	public Worker getWorker() {
		return worker;
	}

	@Override
	public void optimize() {
		for ( IndexManager im : this.allIndexesManager.getIndexManagers() ) {
			im.optimize();
		}
	}

	@Override
	public void optimize(Class entityType) {
		EntityIndexBinding entityIndexBinding = getSafeIndexBindingForEntity( entityType );
		for ( IndexManager im : entityIndexBinding.getIndexManagers() ) {
			im.optimize();
		}
	}

	@Override
	public Analyzer getAnalyzer(String name) {
		final Analyzer analyzer = analyzers.get( name );
		if ( analyzer == null ) {
			throw new SearchException( "Unknown Analyzer definition: " + name );
		}
		return analyzer;
	}

	@Override
	public Analyzer getAnalyzer(Class<?> clazz) {
		EntityIndexBinding entityIndexBinding = getSafeIndexBindingForEntity( clazz );
		DocumentBuilderIndexedEntity<?> builder = entityIndexBinding.getDocumentBuilder();
		return builder.getAnalyzer();
	}

	@Override
	public QueryContextBuilder buildQueryBuilder() {
		return new ConnectedQueryContextBuilder( this );
	}

	@Override
	public Statistics getStatistics() {
		return statistics;
	}

	@Override
	public StatisticsImplementor getStatisticsImplementor() {
		return statistics;
	}

	@Override
	public FilterCachingStrategy getFilterCachingStrategy() {
		return filterCachingStrategy;
	}

	@Override
	public Map<String, Analyzer> getAnalyzers() {
		return analyzers;
	}

	@Override
	public int getCacheBitResultsSize() {
		return cacheBitResultsSize;
	}

	@Override
	public Properties getConfigurationProperties() {
		return configurationProperties;
	}

	@Override
	public FilterDef getFilterDefinition(String name) {
		return filterDefinitions.get( name );
	}

	@Override
	public <T> T requestService(Class<? extends ServiceProvider<T>> provider) {
		return serviceManager.requestService( provider, this );
	}

	@Override
	public void releaseService(Class<? extends ServiceProvider<?>> provider) {
		serviceManager.releaseService( provider );
	}

	@Override
	public int getFilterCacheBitResultsSize() {
		return cacheBitResultsSize;
	}

	@Override
	public Set<Class<?>> getIndexedTypesPolymorphic(Class<?>[] classes) {
		return indexHierarchy.getIndexedClasses( classes );
	}

	@Override
	public BatchBackend makeBatchBackend(MassIndexerProgressMonitor progressMonitor) {
		return new DefaultBatchBackend( this, progressMonitor );
	}

	@Override
	public PolymorphicIndexHierarchy getIndexHierarchy() {
		return indexHierarchy;
	}

	@Override
	public ServiceManager getServiceManager() {
		return serviceManager;
	}

	@Override
	public SearchFactoryImplementor getUninitializedSearchFactory() {
		return this;
	}

	@Override
	public boolean isJMXEnabled() {
		String enableJMX = getConfigurationProperties().getProperty( Environment.JMX_ENABLED );
		return "true".equalsIgnoreCase( enableJMX );
	}

	private String registerMBeans() {
		String mbeanNameSuffix = getConfigurationProperties().getProperty( Environment.JMX_BEAN_SUFFIX );
		String objectName = JMXRegistrar.buildMBeanName(
				StatisticsInfoMBean.STATISTICS_MBEAN_OBJECT_NAME,
				mbeanNameSuffix
		);

		// since the SearchFactory is mutable we might have an already existing MBean which we have to unregister first
		if ( JMXRegistrar.isNameRegistered( objectName ) ) {
			JMXRegistrar.unRegisterMBean( objectName );
		}
		StatisticsInfo statisticsInfo = new StatisticsInfo( statistics );
		JMXRegistrar.registerMBean( statisticsInfo, objectName );
		return objectName;
	}

	@Override
	public boolean isDirtyChecksEnabled() {
		return enableDirtyChecks;
	}

	@Override
	public boolean isStopped() {
		return stopped.get();
	}

	@Override
	public boolean isTransactionManagerExpected() {
		return this.transactionManagerExpected;
	}

	@Override
	public IndexManagerHolder getAllIndexesManager() {
		return getIndexManagerHolder();
	}

	@Override
	public IndexManagerHolder getIndexManagerHolder() {
		return this.allIndexesManager;
	}

	public EntityIndexBinding getSafeIndexBindingForEntity(Class<?> entityType) {
		if ( entityType == null ) {
			throw log.nullIsInvalidIndexedType();
		}
		EntityIndexBinding entityIndexBinding = getIndexBinding( entityType );
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
	public IndexedTypeDescriptor getIndexedTypeDescriptor(Class<?> entityType) {
		IndexedTypeDescriptor typeDescriptor;
		if ( indexedTypeDescriptors.containsKey( entityType ) ) {
			typeDescriptor = indexedTypeDescriptors.get( entityType );
		}
		else {
			EntityIndexBinding indexBinder = indexBindingForEntities.get( entityType );
			IndexedTypeDescriptor indexedTypeDescriptor;
			if ( indexBinder == null ) {
				indexedTypeDescriptor = new IndexedTypeDescriptorForUnindexedType(entityType);
			}
			else {
				indexedTypeDescriptor = new IndexedTypeDescriptorImpl(
						indexBinder.getDocumentBuilder().getMetadata(),
						indexBinder.getIndexManagers()
				);
			}
			indexedTypeDescriptors.put( entityType, indexedTypeDescriptor );
			typeDescriptor = indexedTypeDescriptor;
		}
		return typeDescriptor;
	}

	@Override
	public Set<Class<?>> getIndexedTypes() {
		return indexBindingForEntities.keySet();
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

	@Override
	public boolean isIdProvidedImplicit() {
		return isIdProvidedImplicit;
	}

	@Override
	public IndexManagerFactory getIndexManagerFactory() {
		return indexManagerFactory;
	}

}
