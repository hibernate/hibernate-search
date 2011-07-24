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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.Similarity;
import org.apache.lucene.store.Directory;
import org.hibernate.search.SearchException;
import org.hibernate.search.backend.BackendFactory;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.OptimizeLuceneWork;
import org.hibernate.search.backend.spi.BackendQueueProcessorFactory;
import org.hibernate.search.backend.spi.LuceneIndexingParameters;
import org.hibernate.search.batchindexing.impl.Executors;
import org.hibernate.search.engine.spi.EntityIndexMapping;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.exception.ErrorHandler;
import org.hibernate.search.indexes.CommonPropertiesParse;
import org.hibernate.search.indexes.IndexManager;
import org.hibernate.search.spi.WorkerBuildContext;
import org.hibernate.search.store.DirectoryProvider;
import org.hibernate.search.store.optimization.OptimizerStrategy;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * First implementation will use the "legacy" DirectoryProvider which served us so well.
 * 
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class DirectoryBasedIndexManager implements IndexManager {
	
	private static final Log log = LoggerFactory.make();
	
	private String indexName;
	private final DirectoryProvider directoryProvider;
	private Similarity similarity;
	private ExecutorService backendExecutor;
	private BackendQueueProcessorFactory backend;
	private OptimizerStrategy optimizer;
	private LuceneIndexingParameters inexingParameters;
	private final Set<Class<?>> containedEntityTypes = new HashSet<Class<?>>();
	private ErrorHandler errorHandler;
	private final ReentrantLock dirLock = new ReentrantLock();
	private int maxQueueLength = Executors.QUEUE_MAX_LENGTH;
	private boolean exclusiveIndexUsage;
	
	private SearchFactoryImplementor boundSearchFactory = null;

	private IndexWriterConfig writerConfig;
	
	public DirectoryBasedIndexManager(DirectoryProvider directoryProvider) {
		this.directoryProvider = directoryProvider;
	}

	@Override
	public String getIndexName() {
		return indexName;
	}

	@Override
	public IndexReader openReader() {
		Directory directory = directoryProvider.getDirectory();
		try {
			return IndexReader.open( directory, true );
		}
		catch ( CorruptIndexException e ) {
			log.cantOpenCorruptedIndex( e );
		}
		catch ( IOException e ) {
			log.ioExceptionOnIndex( e );
		}
		throw new SearchException( "Could not open index" );
	}

	@Override
	public void destroy() {
		directoryProvider.stop();
	}

	@Override
	public void initialize(String indexName, Properties cfg, WorkerBuildContext buildContext) {
		this.indexName = indexName;
		directoryProvider.start( this );
		errorHandler = CommonPropertiesParse.createErrorHandler( cfg );
		backendExecutor = BackendFactory.buildWorkerExecutor( cfg, indexName );
		inexingParameters = CommonPropertiesParse.extractIndexingPerformanceOptions( cfg );
		optimizer = CommonPropertiesParse.getOptimizerStrategy( this, cfg );
		backend = BackendFactory.createBackend( this, buildContext, cfg );
		maxQueueLength = CommonPropertiesParse.extractMaxQueueSize( indexName, cfg );
		exclusiveIndexUsage = CommonPropertiesParse.isExclusiveIndexUsageEnabled( indexName, cfg );
	}

	@Override
	public Set<Class<?>> getContainedTypes() {
		return containedEntityTypes;//FIXME
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
		if ( writerConfig != null ) {
			writerConfig.setSimilarity( similarity );
		}
	}

	//Not exposed on the interface
	public DirectoryProvider getDirectoryProvider() {
		return directoryProvider;
	}
	
	@Override
	public void performOperation(LuceneWork singleOperation) {
		ArrayList<LuceneWork> list = new ArrayList<LuceneWork>(1);
		list.add( singleOperation );
		performOperation( list );
	}

	@Override
	public void performOperation(List<LuceneWork> workList) {
		Runnable runnable = backend.getProcessor( workList );
		if ( backendExecutor != null ) {
			backendExecutor.execute( runnable );
		}
		else {
			runnable.run();
		}
	}

	@Override
	public OptimizerStrategy getOptimizerStrategy() {
		return optimizer;
	}

	@Override
	public LuceneIndexingParameters getIndexingParameters() {
		return inexingParameters;
	}

	@Override
	public String toString() {
		return "DirectoryBasedIndexManager [indexName=" + indexName + "]";
	}

	@Override
	public ErrorHandler getErrorHandler() {
		return errorHandler;
	}

	@Override
	public Analyzer getAnalyzer(String name) {
		return boundSearchFactory.getAnalyzer( name );
	}

	@Override
	public EntityIndexMapping<?> getIndexMappingForEntity(Class<?> entityType) {
		return boundSearchFactory.getIndexMappingForEntity( entityType );
	}

	@Override
	public void setBoundSearchFactory(SearchFactoryImplementor boundSearchFactory) {
		this.boundSearchFactory = boundSearchFactory;
	}
	
	//Not exposed on the IndexManager interface
	public Lock getDirectoryModificationLock() {
		return dirLock;
	}

	@Override
	public void addContainedEntity(Class<?> entity) {
		containedEntityTypes.add( entity );
	}

	@Override
	public void optimize() {
		performOperation( new OptimizeLuceneWork() );
	}

	//Not exposed on the IndexManager interface
	public BackendQueueProcessorFactory getBackendQueueProcessorFactory() {
		return backend;
	}

	/**
	 * @return
	 */
	//Not exposed on the IndexManager interface
	public int getMaxQueueLength() {
		return maxQueueLength;
	}

	/**
	 * @return
	 */
	//Not exposed on the IndexManager interface
	public boolean isExclusiveIndexUsage() {
		return this.exclusiveIndexUsage;
	}

	//Not exposed on the IndexManager interface
	public void setIndexWriterConfig(IndexWriterConfig writerConfig) {
		this.writerConfig = writerConfig;
	}

}
