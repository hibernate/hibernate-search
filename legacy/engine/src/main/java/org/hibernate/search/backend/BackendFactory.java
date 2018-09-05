/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend;

import java.util.Properties;

import org.hibernate.search.backend.spi.Backend;
import org.hibernate.search.backend.spi.BackendQueueProcessor;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.indexes.impl.IndexManagerGroupHolder;
import org.hibernate.search.indexes.impl.IndexManagerHolder;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.spi.WorkerBuildContext;

/**
 * Factory to instantiate the {@link BackendQueueProcessor} implementations.
 * <p>
 * Intended to be used by {@link BackendQueueProcessor} implementations looking to delegate
 * to a different implementation.
 *
 * @author Sanne Grinovero (C) 2011 Red Hat Inc.
 * @author Hardy Ferentschik
 */
public final class BackendFactory {
	private BackendFactory() {
		//not allowed
	}

	/**
	 * @param indexManager the index manager
	 * @param buildContext context giving access to required meta data
	 * @param properties all configuration properties
	 * @return A new {@link BackendQueueProcessor} for the given index manager.
	 */
	public static BackendQueueProcessor createBackend(IndexManager indexManager, WorkerBuildContext buildContext, Properties properties) {
		IndexManagerGroupHolder groupHolder = getGroupHolder( indexManager, buildContext );
		Backend backend = groupHolder.getOrCreateBackend( indexManager.getIndexName(), properties, buildContext );
		return backend.createQueueProcessor( indexManager, buildContext );
	}
	/**
	 * @param backendName the name of the backend to be created
	 * @param indexManager the index manager
	 * @param buildContext context giving access to required meta data
	 * @param properties all configuration properties
	 * @return A new {@link BackendQueueProcessor} for the given index manager.
	 */
	public static BackendQueueProcessor createBackend(String backendName, IndexManager indexManager, WorkerBuildContext buildContext,
			Properties properties) {
		IndexManagerGroupHolder groupHolder = getGroupHolder( indexManager, buildContext );
		Backend backend = groupHolder.getOrCreateBackend( backendName, indexManager.getIndexName(), properties, buildContext );
		return backend.createQueueProcessor( indexManager, buildContext );
	}

	private static IndexManagerGroupHolder getGroupHolder(IndexManager indexManager, WorkerBuildContext buildContext) {
		IndexManagerHolder indexManagerHolder = buildContext.getAllIndexesManager();
		return indexManagerHolder.getGroupHolderByIndexManager( indexManager );
	}

	/**
	 * @param properties the configuration to parse
	 *
	 * @return true if the configuration uses sync indexing
	 */
	public static boolean isConfiguredAsSync(Properties properties) {
		// default to sync if none defined
		return !"async".equalsIgnoreCase( properties.getProperty( Environment.WORKER_EXECUTION ) );
	}

}
