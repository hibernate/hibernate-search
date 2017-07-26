/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.indexes.spi;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.locks.Lock;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.similarities.Similarity;
import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.OptimizeLuceneWork;
import org.hibernate.search.backend.impl.lucene.WorkspaceHolder;
import org.hibernate.search.backend.spi.LuceneIndexingParameters;
import org.hibernate.search.cfg.spi.DirectoryProviderService;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.indexes.impl.PropertiesParseHelper;
import org.hibernate.search.indexes.serialization.spi.LuceneWorkSerializer;
import org.hibernate.search.spi.IndexedTypeIdentifier;
import org.hibernate.search.spi.IndexedTypeSet;
import org.hibernate.search.spi.WorkerBuildContext;
import org.hibernate.search.spi.impl.IndexedTypeSets;
import org.hibernate.search.store.DirectoryProvider;
import org.hibernate.search.store.optimization.OptimizerStrategy;
import org.hibernate.search.util.impl.Closer;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * This implementation of {@code IndexManager} is coupled to a
 * {@code DirectoryProvider} and a {@code DirectoryBasedReaderProvider}.
 * <p>
 * This is the base class for all embedded-Lucene index managers.
 *
 * @author Sanne Grinovero (C) 2011 Red Hat Inc.
 */
public class DirectoryBasedIndexManager implements IndexManager {

	private static Log log = LoggerFactory.make();

	private String indexName;
	private DirectoryProvider<?> directoryProvider;
	private Similarity similarity;
	private WorkspaceHolder workspaceHolder;
	private OptimizerStrategy optimizer;
	private LuceneIndexingParameters indexingParameters;
	private IndexedTypeSet containedEntityTypes = IndexedTypeSets.empty();
	private LuceneWorkSerializer serializer;
	private ExtendedSearchIntegrator boundSearchIntegrator = null;
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
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.push( readers::stop );
			closer.push( workspaceHolder::close );
			closer.push( directoryProvider::stop );
			if ( serializer != null ) {
				closer.push( serviceManager::releaseService, LuceneWorkSerializer.class );
			}
		}
	}

	@Override
	public void initialize(String indexName, Properties properties, Similarity similarity, WorkerBuildContext buildContext) {
		this.serviceManager = buildContext.getServiceManager();
		this.indexName = indexName;
		this.similarity = similarity;
		this.directoryProvider = createDirectoryProvider( indexName, properties, buildContext );
		this.indexingParameters = PropertiesParseHelper.extractIndexingPerformanceOptions( properties );
		this.optimizer = PropertiesParseHelper.getOptimizerStrategy( this, properties, buildContext );
		this.workspaceHolder = createWorkspaceHolder( indexName, properties, buildContext );
		this.directoryProvider.start( this );
		this.readers = createIndexReader( indexName, properties, buildContext );
	}

	@Override
	public IndexedTypeSet getContainedTypes() {
		return containedEntityTypes;
	}

	@Override
	public Similarity getSimilarity() {
		return similarity;
	}

	@Override
	public void performStreamOperation(LuceneWork singleOperation, IndexingMonitor monitor, boolean forceAsync) {
		//TODO implement async
		workspaceHolder.applyStreamWork( singleOperation, monitor );
	}

	@Override
	public void awaitAsyncProcessingCompletion() {
		//TODO async not implemented
	}

	@Override
	public void performOperations(List<LuceneWork> workList, IndexingMonitor monitor) {
		if ( log.isDebugEnabled() ) {
			log.debug( "Applying work via workspace holder of type " + workspaceHolder.getClass() );
		}
		workspaceHolder.applyWork( workList, monitor );
	}

	@Override
	public String toString() {
		return "DirectoryBasedIndexManager [indexName=" + indexName + "]";
	}

	@Override
	public Analyzer getAnalyzer(String name) {
		return boundSearchIntegrator.getAnalyzer( name );
	}

	@Override
	public void setSearchFactory(ExtendedSearchIntegrator boundSearchIntegrator) {
		this.boundSearchIntegrator = boundSearchIntegrator;
		triggerWorkspaceReconfiguration();
	}

	@Override
	public void addContainedEntity(IndexedTypeIdentifier entity) {
		final IndexedTypeSet oldSet = containedEntityTypes;
		final IndexedTypeSet newSet = IndexedTypeSets.composite( oldSet, entity );
		if ( ! oldSet.equals( newSet ) ) {
			this.containedEntityTypes = newSet;
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
			serializer = requestSerializer();
			log.indexManagerUsesSerializationService( this.indexName, this.serializer.describeSerializer() );
		}
		return serializer;
	}

	@Override
	public void flushAndReleaseResources() {
		workspaceHolder.flushAndReleaseResources();
	}

	private LuceneWorkSerializer requestSerializer() {
		try {
			return serviceManager.requestService( LuceneWorkSerializer.class );
		}
		catch (SearchException se) {
			throw log.serializationProviderNotFoundException( se );
		}
	}

	//Not exposed on the IndexManager interface
	public WorkspaceHolder getWorkspaceHolder() {
		return workspaceHolder;
	}

	//Not exposed on the IndexManager interface
	public EntityIndexBinding getIndexBinding(IndexedTypeIdentifier type) {
		return boundSearchIntegrator.getIndexBinding( type );
	}

	//Not exposed on the IndexManager interface
	public Lock getDirectoryModificationLock() {
		return workspaceHolder.getExclusiveWriteLock();
	}

	//Not exposed on the interface
	public DirectoryProvider<?> getDirectoryProvider() {
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
		if ( boundSearchIntegrator != null ) { //otherwise it's too early
			workspaceHolder.indexMappingChanged();
		}
	}

	protected WorkspaceHolder createWorkspaceHolder(String indexName, Properties cfg, WorkerBuildContext buildContext) {
		WorkspaceHolder backend = new WorkspaceHolder();
		backend.initialize( cfg, buildContext, this );
		return backend;
	}

	protected DirectoryBasedReaderProvider createIndexReader(String indexName, Properties cfg, WorkerBuildContext buildContext) {
		return PropertiesParseHelper.createDirectoryBasedReaderProvider( this, cfg, buildContext );
	}

	protected DirectoryProvider<?> createDirectoryProvider(String indexName, Properties cfg, WorkerBuildContext buildContext) {
		try {
			DirectoryProviderService directoryProviderService = serviceManager.requestService( DirectoryProviderService.class );
			return directoryProviderService.create( cfg, indexName, buildContext );
		}
		finally {
			serviceManager.releaseService( DirectoryProviderService.class );
		}
	}

	@Override
	public IndexManagerType getIndexManagerType() {
		return LuceneEmbeddedIndexManagerType.INSTANCE;
	}

}
