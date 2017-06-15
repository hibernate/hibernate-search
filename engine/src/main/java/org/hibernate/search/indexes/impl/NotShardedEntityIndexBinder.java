/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.indexes.impl;

import java.util.Properties;

import org.hibernate.search.engine.impl.MutableEntityIndexBinding;
import org.hibernate.search.indexes.interceptor.EntityIndexingInterceptor;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.indexes.spi.IndexManagerSelector;
import org.hibernate.search.spi.IndexedTypeIdentifier;
import org.hibernate.search.spi.WorkerBuildContext;


/**
 * @author Yoann Rodiere
 */
class NotShardedEntityIndexBinder implements EntityIndexBinder {
	private static final String INDEX_BACKEND_NAME_SEPARATOR = "#";

	private final Properties properties;

	public NotShardedEntityIndexBinder(Properties properties) {
		this.properties = properties;
	}

	@Override
	public MutableEntityIndexBinding bind(IndexManagerGroupHolder holder, IndexedTypeIdentifier entityType,
			EntityIndexingInterceptor<?> interceptor, WorkerBuildContext buildContext) {
		IndexManager indexManager = holder.getOrCreateIndexManager( null, properties, entityType, buildContext );

		IndexManagerSelector selector = new NotShardedIndexManagerSelector( indexManager );

		return new MutableEntityIndexBinding( holder, selector, null, interceptor );
	}

	@Override
	public String createBackendIdentifier(String backendName, String indexName) {
		/*
		 * We integrate the backend name in the ID, in order to
		 * handle the case where a backend delegates to another implementation.
		 */
		return indexName + INDEX_BACKEND_NAME_SEPARATOR + backendName;
	}

}
