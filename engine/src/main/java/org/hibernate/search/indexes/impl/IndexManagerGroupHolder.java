/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.indexes.impl;

import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.lucene.search.similarities.Similarity;
import org.hibernate.search.backend.BackendFactory;
import org.hibernate.search.backend.spi.BackendQueueProcessor;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.cfg.spi.IndexManagerFactory;
import org.hibernate.search.engine.impl.MutableEntityIndexBinding;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.engine.service.spi.ServiceReference;
import org.hibernate.search.indexes.interceptor.EntityIndexingInterceptor;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.indexes.spi.IndexManagerType;
import org.hibernate.search.spi.WorkerBuildContext;
import org.hibernate.search.util.StringHelper;
import org.hibernate.search.util.configuration.impl.MaskedProperty;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Holds references to all index managers representing either:
 * <ul>
 * <li>The only index manager of a given index (no sharding)
 * <li>The index managers for each shard of a given index (sharding)
 * </ul>
 *
 * @author Yoann Rodiere
 */
public class IndexManagerGroupHolder implements AutoCloseable {
	private static final Log log = LoggerFactory.make();

	private static final String INDEX_SHARD_ID_SEPARATOR = ".";

	private final IndexManagerHolder parentHolder;

	private final String indexNameBase;

	private final Similarity similarity;
	private final IndexManagerType indexManagerType;
	private final EntityIndexBinder entityIndexBinder;

	private final ConcurrentMap<String, IndexManager> indexManagersRegistry = new ConcurrentHashMap<>();

	public IndexManagerGroupHolder(IndexManagerHolder parentHolder,
			String indexNameBase,
			Similarity similarity,
			IndexManagerType indexManagerType,
			EntityIndexBinder entityIndexBinder) {
		super();
		this.parentHolder = parentHolder;
		this.indexNameBase = indexNameBase;
		this.similarity = similarity;
		this.indexManagerType = indexManagerType;
		this.entityIndexBinder = entityIndexBinder;
	}

	@Override
	public synchronized void close() {
		for ( IndexManager indexManager : indexManagersRegistry.values() ) {
			indexManager.destroy();
		}
		indexManagersRegistry.clear();
	}

	public Similarity getSimilarity() {
		return similarity;
	}

	public IndexManagerType getIndexManagerType() {
		return indexManagerType;
	}

	MutableEntityIndexBinding bind(Class<?> entityType, EntityIndexingInterceptor<?> interceptor,
			WorkerBuildContext buildContext) {
		// This will create the binding, but also initialize the indexes as necessary
		return entityIndexBinder.bind( this, entityType, interceptor, buildContext );
	}

	IndexManager getOrCreateIndexManager(String shardName, Properties indexProperties, Class<?> entityType,
			WorkerBuildContext context) {
		String indexName = indexNameBase;
		if ( shardName != null ) {
			indexName += INDEX_SHARD_ID_SEPARATOR + shardName;
		}

		IndexManager indexManager = indexManagersRegistry.get( indexName );
		if ( indexManager != null ) {
			indexManager.addContainedEntity( entityType );
			return indexManager;
		}

		synchronized (this) {
			indexManager = indexManagersRegistry.get( indexName );
			if ( indexManager != null ) {
				indexManager.addContainedEntity( entityType );
				return indexManager;
			}
			if ( shardName != null ) {
				indexProperties = new MaskedProperty( indexProperties, shardName, indexProperties );
			}
			indexManager = doCreateIndexManager( indexName, entityType, indexProperties, context );
		}

		return indexManager;
	}

	private synchronized IndexManager doCreateIndexManager(String indexName, Class<?> entityType,
			Properties indexProperties, WorkerBuildContext workerBuildContext) {
		ExtendedSearchIntegrator activeIntegrator = null;

		if ( workerBuildContext == null ) {
			/*
			 * The index manager is being dynamically requested at runtime
			 * (not during bootstrapping).
			 * We will need to set to integrator on the indexManager to initialize it
			 * (see further down).
			 */
			activeIntegrator = parentHolder.getActiveSearchIntegrator();
			workerBuildContext = toWorkerBuildContext( activeIntegrator );
		}

		// get hold of the index manager factory via the service manager
		ServiceManager serviceManager = workerBuildContext.getServiceManager();

		// create IndexManager instance via the index manager factory
		String indexManagerImplementationName = indexProperties.getProperty( Environment.INDEX_MANAGER_IMPL_NAME );
		final IndexManager manager;
		try ( ServiceReference<IndexManagerFactory> indexManagerFactoryRef
				= serviceManager.requestReference( IndexManagerFactory.class ) ) {
			IndexManagerFactory indexManagerFactory = indexManagerFactoryRef.get();
			if ( StringHelper.isEmpty( indexManagerImplementationName ) ) {
				manager = indexManagerFactory.createDefaultIndexManager();
			}
			else {
				manager = indexManagerFactory.createIndexManagerByName( indexManagerImplementationName );
			}
		}

		// init the IndexManager
		try {
			manager.initialize( indexName, indexProperties, similarity, workerBuildContext );
		}
		catch (Exception e) {
			throw log.unableToInitializeIndexManager( indexName, e );
		}

		indexManagersRegistry.put( indexName, manager );

		BackendQueueProcessor backendQueueProcessor = BackendFactory.createBackend( manager, workerBuildContext, indexProperties );
		parentHolder.register( indexName, manager, backendQueueProcessor );

		manager.addContainedEntity( entityType );

		if ( activeIntegrator != null ) {
			manager.setSearchFactory( activeIntegrator );
		}

		return manager;
	}

	private WorkerBuildContext toWorkerBuildContext(ExtendedSearchIntegrator integrator) {
		//known implementations of SearchFactory passed are MutableSearchFactory and ImmutableSearchFactory
		if ( integrator instanceof WorkerBuildContext ) {
			return (WorkerBuildContext) integrator;
		}
		else {
			throw log.assertionFailureCannotCastToWorkerBuilderContext( integrator.getClass() );
		}
	}

}
