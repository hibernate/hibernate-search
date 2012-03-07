/* 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.search.indexes.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.locks.Lock;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.Similarity;
import org.hibernate.search.backend.BackendFactory;
import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.OptimizeLuceneWork;
import org.hibernate.search.backend.spi.BackendQueueProcessor;
import org.hibernate.search.backend.spi.LuceneIndexingParameters;
import org.hibernate.search.engine.spi.EntityIndexBinder;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.indexes.serialization.spi.LuceneWorkSerializer;
import org.hibernate.search.indexes.spi.DirectoryBasedReaderProvider;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.indexes.spi.ReaderProvider;
import org.hibernate.search.spi.WorkerBuildContext;
import org.hibernate.search.store.DirectoryProvider;
import org.hibernate.search.store.impl.DirectoryProviderFactory;
import org.hibernate.search.store.optimization.OptimizerStrategy;

/**
 * This implementation of IndexManager is coupled to a
 * DirectoryProvider and a DirectoryBasedReaderProvider
 * 
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class DirectoryBasedIndexManager implements IndexManager {
	
	private String indexName;
	private DirectoryProvider directoryProvider;
	private Similarity similarity;
	private BackendQueueProcessor backend;
	private OptimizerStrategy optimizer;
	private LuceneIndexingParameters indexingParameters;
	private final Set<Class<?>> containedEntityTypes = new HashSet<Class<?>>();
	private LuceneWorkSerializer serializer;
	private SearchFactoryImplementor boundSearchFactory = null;
	private DirectoryBasedReaderProvider readers = null;
	private IndexWriterConfig writerConfig;

	@Override
	public String getIndexName() {
		return indexName;
	}

	@Override
	public ReaderProvider getReaderProvider() {
		return readers;
	}

	@Override
	public void destroy() {
		readers.stop();
		backend.close();
		directoryProvider.stop();
	}

	@Override
	public void initialize(String indexName, Properties cfg, WorkerBuildContext buildContext) {
		this.indexName = indexName;
		directoryProvider = createDirectoryProvider( indexName, cfg, buildContext );
		indexingParameters = CommonPropertiesParse.extractIndexingPerformanceOptions( cfg );
		optimizer = CommonPropertiesParse.getOptimizerStrategy( this, cfg );
		backend = createBackend( indexName, cfg, buildContext );
		directoryProvider.start( this );
		readers = createIndexReader( indexName, cfg, buildContext );
		serializer = BackendFactory.createSerializer( indexName, cfg, buildContext );
	}

	@Override
	public Set<Class<?>> getContainedTypes() {
		return containedEntityTypes;
	}

	@Override
	public Similarity getSimilarity() {
		return similarity;
	}

	@Override
	public void setSimilarity(Similarity newSimilarity) {
		this.similarity = newSimilarity;
		//TODO fix similarity: it's currently being set multiple times before reaching the final
		// configuration, possibly *after* the backend was created, so we have to fix the backend too.
		triggerWorkspaceReconfiguration();
		if ( writerConfig != null ) {
			writerConfig.setSimilarity( similarity );
		}
	}

	@Override
	public void performStreamOperation(LuceneWork singleOperation, IndexingMonitor monitor,  boolean forceAsync) {
		//TODO implement async
		backend.applyStreamWork( singleOperation, monitor );
	}

	@Override
	public void performOperations(List<LuceneWork> workList, IndexingMonitor monitor) {
		backend.applyWork( workList, monitor );
	}

	@Override
	public String toString() {
		return "DirectoryBasedIndexManager [indexName=" + indexName + "]";
	}

	@Override
	public Analyzer getAnalyzer(String name) {
		return boundSearchFactory.getAnalyzer( name );
	}

	@Override
	public void setSearchFactory(SearchFactoryImplementor boundSearchFactory) {
		this.boundSearchFactory = boundSearchFactory;
		triggerWorkspaceReconfiguration();
	}
	
	@Override
	public void addContainedEntity(Class<?> entity) {
		containedEntityTypes.add( entity );
		triggerWorkspaceReconfiguration();
	}

	@Override
	public void optimize() {
		performStreamOperation( OptimizeLuceneWork.INSTANCE, null, false );
	}

	//Not exposed on the IndexManager interface
	public BackendQueueProcessor getBackendQueueProcessor() {
		return backend;
	}

	//Not exposed on the IndexManager interface
	public void setIndexWriterConfig(IndexWriterConfig writerConfig) {
		this.writerConfig = writerConfig;
	}
	
	//Not exposed on the IndexManager interface
	public EntityIndexBinder getIndexBindingForEntity(Class<?> entityType) {
		return boundSearchFactory.getIndexBindingForEntity( entityType );
	}
	
	//Not exposed on the IndexManager interface
	public Lock getDirectoryModificationLock() {
		return backend.getExclusiveWriteLock();
	}

	//Not exposed on the interface
	public DirectoryProvider getDirectoryProvider() {
		return directoryProvider;
	}

	//Not exposed on the interface
	public OptimizerStrategy getOptimizerStrategy() {
		return optimizer;
	}

	//Not exposed on the interface
	public LuceneIndexingParameters getIndexingParameters() {
		return indexingParameters;
	}

	@Override
	public LuceneWorkSerializer getSerializer() {
		return serializer;
	}

	private void triggerWorkspaceReconfiguration() {
		if ( boundSearchFactory != null ) { //otherwise it's too early
			backend.indexMappingChanged();
		}
	}

	/**
	 * extensions points from {@link #initialize(String, Properties, WorkerBuildContext)}
	 */

	protected BackendQueueProcessor createBackend(String indexName, Properties cfg, WorkerBuildContext buildContext) {
		return BackendFactory.createBackend( this, buildContext, cfg );
	}

	protected DirectoryBasedReaderProvider createIndexReader(String indexName, Properties cfg, WorkerBuildContext buildContext) {
		return  CommonPropertiesParse.createDirectoryBasedReaderProvider( this, cfg );
	}

	protected DirectoryProvider createDirectoryProvider(String indexName, Properties cfg, WorkerBuildContext buildContext) {
		return  DirectoryProviderFactory.createDirectoryProvider( indexName, cfg, buildContext );
	}

}
