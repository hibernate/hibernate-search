/* $Id$
 * 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * Copyright (c) 2009, Red Hat, Inc. and/or its affiliates or third-party contributors as
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.Similarity;
import org.slf4j.Logger;

import org.hibernate.annotations.common.AssertionFailure;
import org.hibernate.search.Environment;
import org.hibernate.search.SearchException;
import org.hibernate.search.Version;
import org.hibernate.search.backend.BackendQueueProcessorFactory;
import org.hibernate.search.backend.LuceneIndexingParameters;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.OptimizeLuceneWork;
import org.hibernate.search.backend.Worker;
import org.hibernate.search.backend.configuration.MaskedProperty;
import org.hibernate.search.backend.impl.batchlucene.BatchBackend;
import org.hibernate.search.backend.impl.batchlucene.LuceneBatchBackend;
import org.hibernate.search.batchindexing.MassIndexerProgressMonitor;
import org.hibernate.search.engine.DocumentBuilderContainedEntity;
import org.hibernate.search.engine.DocumentBuilderIndexedEntity;
import org.hibernate.search.engine.FilterDef;
import org.hibernate.search.engine.SearchFactoryImplementor;
import org.hibernate.search.exception.ErrorHandler;
import org.hibernate.search.filter.FilterCachingStrategy;
import org.hibernate.search.query.dsl.v2.QueryContextBuilder;
import org.hibernate.search.query.dsl.v2.impl.ConnectedQueryContextBuilder;
import org.hibernate.search.reader.ReaderProvider;
import org.hibernate.search.spi.WorkerBuildContext;
import org.hibernate.search.store.DirectoryProvider;
import org.hibernate.search.store.optimization.OptimizerStrategy;
import org.hibernate.search.util.LoggerFactory;
import org.hibernate.search.util.PluginLoader;
import org.hibernate.util.StringHelper;

/**
 * @author Emmanuel Bernard
 */
public class SearchFactoryImpl implements SearchFactoryImplementor, WorkerBuildContext {

	static {
		Version.touch();
	}

	private static final Logger log = LoggerFactory.make();

	private final Map<Class<?>, DocumentBuilderIndexedEntity<?>> documentBuildersIndexedEntities;
	private final Map<Class<?>, DocumentBuilderContainedEntity<?>> documentBuildersContainedEntities;
	//keep track of the index modifiers per DirectoryProvider since multiple entity can use the same directory provider
	private final Map<DirectoryProvider<?>, DirectoryProviderData> dirProviderData;
	private final Worker worker;
	private final ReaderProvider readerProvider;
	private final BackendQueueProcessorFactory backendQueueProcessorFactory;
	private final Map<String, FilterDef> filterDefinitions;
	private final FilterCachingStrategy filterCachingStrategy;
	private final Map<String, Analyzer> analyzers;
	private final AtomicBoolean stopped = new AtomicBoolean( false );
	private final int cacheBitResultsSize;
	private final Properties configurationProperties;
	private final ErrorHandler errorHandler;
	private final PolymorphicIndexHierarchy indexHierarchy;

	/**
	 * Each directory provider (index) can have its own performance settings.
	 */
	private final Map<DirectoryProvider, LuceneIndexingParameters> dirProviderIndexingParams;
	private final String indexingStrategy;

	public SearchFactoryImpl(SearchFactoryBuilder cfg) {
		this.analyzers = cfg.analyzers;
		this.backendQueueProcessorFactory = cfg.backendQueueProcessorFactory;
		this.cacheBitResultsSize = cfg.cacheBitResultsSize;
		this.configurationProperties = cfg.configurationProperties;
		this.dirProviderData = cfg.dirProviderData;
		this.dirProviderIndexingParams = cfg.dirProviderIndexingParams;
		this.documentBuildersIndexedEntities = cfg.documentBuildersIndexedEntities;
		this.documentBuildersContainedEntities = cfg.documentBuildersContainedEntities;
		this.errorHandler = cfg.errorHandler;
		this.filterCachingStrategy = cfg.filterCachingStrategy;
		this.filterDefinitions = cfg.filterDefinitions;
		this.indexHierarchy = cfg.indexHierarchy;
		this.indexingStrategy = cfg.indexingStrategy;
		this.readerProvider = cfg.readerProvider;
		this.worker = cfg.worker;
	}


	public BackendQueueProcessorFactory getBackendQueueProcessorFactory() {
		return backendQueueProcessorFactory;
	}

	public String getIndexingStrategy() {
		return indexingStrategy;
	}

	public void close() {
		if ( stopped.compareAndSet( false, true ) ) {  //make sure we only sop once
			try {
				worker.close();
			}
			catch ( Exception e ) {
				log.error( "Worker raises an exception on close()", e );
			}

			try {
				readerProvider.destroy();
			}
			catch ( Exception e ) {
				log.error( "ReaderProvider raises an exception on destroy()", e );
			}

			//TODO move directory provider cleaning to DirectoryProviderFactory
			for ( DirectoryProvider dp : getDirectoryProviders() ) {
				try {
					dp.stop();
				}
				catch ( Exception e ) {
					log.error( "DirectoryProvider raises an exception on stop() ", e );
				}
			}
		}
	}

	public Set<Class<?>> getClassesInDirectoryProvider(DirectoryProvider<?> directoryProvider) {
		return Collections.unmodifiableSet( dirProviderData.get( directoryProvider ).getClasses() );
	}

	public Map<Class<?>, DocumentBuilderIndexedEntity<?>> getDocumentBuildersIndexedEntities() {
		return documentBuildersIndexedEntities;
	}

	@SuppressWarnings("unchecked")
	public <T> DocumentBuilderIndexedEntity<T> getDocumentBuilderIndexedEntity(Class<T> entityType) {
		return ( DocumentBuilderIndexedEntity<T> ) documentBuildersIndexedEntities.get( entityType );
	}

	@SuppressWarnings("unchecked")
	public <T> DocumentBuilderContainedEntity<T> getDocumentBuilderContainedEntity(Class<T> entityType) {
		return ( DocumentBuilderContainedEntity<T> ) documentBuildersContainedEntities.get( entityType );
	}

	public Set<DirectoryProvider<?>> getDirectoryProviders() {
		return this.dirProviderData.keySet();
	}

	public Worker getWorker() {
		return worker;
	}

	public void setBackendQueueProcessorFactory(BackendQueueProcessorFactory backendQueueProcessorFactory) {
		throw new AssertionFailure( "SearchFactoryImpl is immutable: should never be called");
	}

	public OptimizerStrategy getOptimizerStrategy(DirectoryProvider<?> provider) {
		return dirProviderData.get( provider ).getOptimizerStrategy();
	}

	public LuceneIndexingParameters getIndexingParameters(DirectoryProvider<?> provider) {
		return dirProviderIndexingParams.get( provider );
	}

	public ReaderProvider getReaderProvider() {
		return readerProvider;
	}

	public DirectoryProvider[] getDirectoryProviders(Class<?> entity) {
		DocumentBuilderIndexedEntity<?> documentBuilder = getDocumentBuilderIndexedEntity( entity );
		return documentBuilder == null ? null : documentBuilder.getDirectoryProviders();
	}

	public void optimize() {
		Set<Class<?>> clazzs = getDocumentBuildersIndexedEntities().keySet();
		for ( Class clazz : clazzs ) {
			optimize( clazz );
		}
	}

	public void optimize(Class entityType) {
		if ( !getDocumentBuildersIndexedEntities().containsKey( entityType ) ) {
			throw new SearchException( "Entity not indexed: " + entityType );
		}
		List<LuceneWork> queue = new ArrayList<LuceneWork>( 1 );
		queue.add( new OptimizeLuceneWork( entityType ) );
		getBackendQueueProcessorFactory().getProcessor( queue ).run();
	}

	public Analyzer getAnalyzer(String name) {
		final Analyzer analyzer = analyzers.get( name );
		if ( analyzer == null ) {
			throw new SearchException( "Unknown Analyzer definition: " + name );
		}
		return analyzer;
	}

	public Analyzer getAnalyzer(Class<?> clazz) {
		if ( clazz == null ) {
			throw new IllegalArgumentException( "A class has to be specified for retrieving a scoped analyzer" );
		}

		DocumentBuilderIndexedEntity<?> builder = documentBuildersIndexedEntities.get( clazz );
		if ( builder == null ) {
			throw new IllegalArgumentException(
					"Entity for which to retrieve the scoped analyzer is not an @Indexed entity: " + clazz.getName()
			);
		}

		return builder.getAnalyzer();
	}

	public QueryContextBuilder buildQueryBuilder() {
		return new ConnectedQueryContextBuilder( this );
	}

	public FilterCachingStrategy getFilterCachingStrategy() {
		return filterCachingStrategy;
	}

	public FilterDef getFilterDefinition(String name) {
		return filterDefinitions.get( name );
	}

	public ReentrantLock getDirectoryProviderLock(DirectoryProvider<?> dp) {
		return this.dirProviderData.get( dp ).getDirLock();
	}

	public int getFilterCacheBitResultsSize() {
		return cacheBitResultsSize;
	}

	public Set<Class<?>> getIndexedTypesPolymorphic(Class<?>[] classes) {
		return indexHierarchy.getIndexedClasses( classes );
	}
	
	public BatchBackend makeBatchBackend(MassIndexerProgressMonitor progressMonitor) {
		BatchBackend batchBackend;
		String impl = configurationProperties.getProperty( Environment.BATCH_BACKEND );
		if ( StringHelper.isEmpty( impl ) || "LuceneBatch".equalsIgnoreCase( impl ) ) {
			batchBackend = new LuceneBatchBackend();
		}
		else {
			batchBackend = PluginLoader.instanceFromName( BatchBackend.class, impl, SearchFactoryImpl.class,
					"batchbackend" );
		}
		Properties batchBackendConfiguration = new MaskedProperty(
				this.configurationProperties, Environment.BATCH_BACKEND );
		batchBackend.initialize( batchBackendConfiguration, progressMonitor, this );
		return batchBackend;
	}

	public Similarity getSimilarity(DirectoryProvider<?> provider) {
		Similarity similarity = dirProviderData.get( provider ).getSimilarity();
		if ( similarity == null ) throw new SearchException( "Assertion error: a similarity should be defined for each provider" );
		return similarity;
	}

	public boolean isExclusiveIndexUsageEnabled(DirectoryProvider<?> provider) {
		return dirProviderData.get( provider ).isExclusiveIndexUsage();
	}

	public ErrorHandler getErrorHandler() {
		return errorHandler;
	}

	public SearchFactoryImplementor getUninitializedSearchFactory() {
		return this;
	}
}
