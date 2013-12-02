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
import org.apache.lucene.search.Similarity;
import org.hibernate.search.backend.BackendFactory;
import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.OptimizeLuceneWork;
import org.hibernate.search.backend.spi.BackendQueueProcessor;
import org.hibernate.search.backend.spi.LuceneIndexingParameters;
import org.hibernate.search.engine.ServiceManager;
import org.hibernate.search.engine.impl.EmptyBuildContext;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.indexes.serialization.spi.LuceneWorkSerializer;
import org.hibernate.search.indexes.serialization.spi.SerializerService;
import org.hibernate.search.indexes.spi.DirectoryBasedReaderProvider;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.indexes.spi.ReaderProvider;
import org.hibernate.search.spi.WorkerBuildContext;
import org.hibernate.search.store.DirectoryProvider;
import org.hibernate.search.store.impl.DirectoryProviderFactory;
import org.hibernate.search.store.optimization.OptimizerStrategy;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * This implementation of {@code IndexManager} is coupled to a
 * {@code DirectoryProvider} and a {@code DirectoryBasedReaderProvider}.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class DirectoryBasedIndexManager implements IndexManager {

	private static Log log = LoggerFactory.make();

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
	private ServiceManager serviceManager;

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
		if ( serializer != null ) {
			serviceManager.releaseService( SerializerService.class );
		}
	}

	@Override
	public void initialize(String indexName, Properties properties, Similarity similarity, WorkerBuildContext buildContext) {
		this.indexName = indexName;
		this.similarity = similarity;
		this.directoryProvider = createDirectoryProvider( indexName, properties, buildContext );
		this.indexingParameters = PropertiesParseHelper.extractIndexingPerformanceOptions( properties );
		this.optimizer = PropertiesParseHelper.getOptimizerStrategy( this, properties );
		this.backend = createBackend( indexName, properties, buildContext );
		this.directoryProvider.start( this );
		this.readers = createIndexReader( indexName, properties, buildContext );
		this.serviceManager = buildContext.getServiceManager();
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
	public void performStreamOperation(LuceneWork singleOperation, IndexingMonitor monitor, boolean forceAsync) {
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
		if ( containedEntityTypes.add( entity ) ) {
			triggerWorkspaceReconfiguration();
		}
	}

	@Override
	public void optimize() {
		performStreamOperation( OptimizeLuceneWork.INSTANCE, null, false );
	}

	@Override
	public LuceneWorkSerializer getSerializer() {
		if ( serializer == null ) {
			EmptyBuildContext buildContext = new EmptyBuildContext( serviceManager, boundSearchFactory );
			serializer = serviceManager.requestService( SerializerService.class, buildContext );
			log.indexManagerUsesSerializationService( this.indexName, this.serializer.describeSerializer() );
		}
		return serializer;
	}

	//Not exposed on the IndexManager interface
	public BackendQueueProcessor getBackendQueueProcessor() {
		return backend;
	}

	//Not exposed on the IndexManager interface
	public EntityIndexBinding getIndexBinding(Class<?> entityType) {
		return boundSearchFactory.getIndexBinding( entityType );
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

	private void triggerWorkspaceReconfiguration() {
		if ( boundSearchFactory != null ) { //otherwise it's too early
			backend.indexMappingChanged();
		}
	}

	protected BackendQueueProcessor createBackend(String indexName, Properties cfg, WorkerBuildContext buildContext) {
		return BackendFactory.createBackend( this, buildContext, cfg );
	}

	protected DirectoryBasedReaderProvider createIndexReader(String indexName, Properties cfg, WorkerBuildContext buildContext) {
		return PropertiesParseHelper.createDirectoryBasedReaderProvider( this, cfg );
	}

	protected DirectoryProvider createDirectoryProvider(String indexName, Properties cfg, WorkerBuildContext buildContext) {
		return DirectoryProviderFactory.createDirectoryProvider( indexName, cfg, buildContext );
	}

}
