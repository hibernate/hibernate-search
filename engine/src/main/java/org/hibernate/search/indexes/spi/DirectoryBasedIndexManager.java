/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.indexes.spi;

import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.locks.Lock;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.similarities.Similarity;

import org.hibernate.search.backend.BackendFactory;
import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.OptimizeLuceneWork;
import org.hibernate.search.backend.spi.BackendQueueProcessor;
import org.hibernate.search.backend.spi.LuceneIndexingParameters;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.cfg.spi.DirectoryProviderService;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.indexes.impl.PropertiesParseHelper;
import org.hibernate.search.indexes.serialization.impl.LuceneWorkSerializerImpl;
import org.hibernate.search.indexes.serialization.spi.LuceneWorkSerializer;
import org.hibernate.search.indexes.serialization.spi.SerializationProvider;
import org.hibernate.search.spi.WorkerBuildContext;
import org.hibernate.search.store.DirectoryProvider;
import org.hibernate.search.store.optimization.OptimizerStrategy;
import org.hibernate.search.util.StringHelper;
import org.hibernate.search.util.configuration.impl.ConfigurationParseHelper;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * This implementation of {@code IndexManager} is coupled to a
 * {@code DirectoryProvider} and a {@code DirectoryBasedReaderProvider}.
 *
 * @author Sanne Grinovero (C) 2011 Red Hat Inc.
 */
public class DirectoryBasedIndexManager implements IndexManager {

	private static Log log = LoggerFactory.make();

	private String indexName;
	private DirectoryProvider<?> directoryProvider;
	private Similarity similarity;
	private BackendQueueProcessor backend;
	private OptimizerStrategy optimizer;
	private LuceneIndexingParameters indexingParameters;
	private final Set<Class<?>> containedEntityTypes = new HashSet<Class<?>>();
	private LuceneWorkSerializer serializer;
	private SerializationProvider serializationProvider;
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
		readers.stop();
		backend.close();
		directoryProvider.stop();
		if ( serializationProvider != null ) {
			serviceManager.releaseService( SerializationProvider.class );
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
		this.backend = createBackend( indexName, properties, buildContext );
		boolean enlistInTransaction = ConfigurationParseHelper.getBooleanValue(
				properties,
				Environment.WORKER_ENLIST_IN_TRANSACTION,
				false
		);
		if ( enlistInTransaction && ! ( backend instanceof BackendQueueProcessor.Transactional ) ) {
			// We are expecting to use a transactional worker but the backend is not
			// this is war!
			// TODO would be better to have this check in the indexManager factory but we need access to the backend
			String backend = properties.getProperty( Environment.WORKER_BACKEND );
			backend = StringHelper.isEmpty( backend ) ? "lucene" : backend;
			throw log.backendNonTransactional( indexName, backend );

		}
		this.directoryProvider.start( this );
		this.readers = createIndexReader( indexName, properties, buildContext );
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
		if ( log.isDebugEnabled() ) {
			log.debug( "Sending work to backend of type " + backend.getClass() );
		}
		backend.applyWork( workList, monitor );
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
			serializationProvider = requestSerializationProvider();
			serializer = new LuceneWorkSerializerImpl( serializationProvider, boundSearchIntegrator );
			log.indexManagerUsesSerializationService( this.indexName, this.serializer.describeSerializer() );
		}
		return serializer;
	}

	private SerializationProvider requestSerializationProvider() {
		try {
			return serviceManager.requestService( SerializationProvider.class );
		}
		catch (SearchException se) {
			throw log.serializationProviderNotFoundException( se );
		}
	}

	//Not exposed on the IndexManager interface
	public BackendQueueProcessor getBackendQueueProcessor() {
		return backend;
	}

	//Not exposed on the IndexManager interface
	public EntityIndexBinding getIndexBinding(Class<?> entityType) {
		return boundSearchIntegrator.getIndexBinding( entityType );
	}

	//Not exposed on the IndexManager interface
	public Lock getDirectoryModificationLock() {
		return backend.getExclusiveWriteLock();
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
			backend.indexMappingChanged();
		}
	}

	protected BackendQueueProcessor createBackend(String indexName, Properties cfg, WorkerBuildContext buildContext) {
		return BackendFactory.createBackend( this, buildContext, cfg );
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

}
