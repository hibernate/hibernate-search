/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.indexes.impl;

import org.hibernate.search.engine.impl.MutableEntityIndexBinding;
import org.hibernate.search.indexes.interceptor.EntityIndexingInterceptor;
import org.hibernate.search.spi.IndexedTypeIdentifier;
import org.hibernate.search.spi.WorkerBuildContext;

/**
 * Manages the binding of entities to index, i.e. initializes the environment
 * when a new entity type is added to an {@link IndexManagerGroupHolder}.
 *
 * @author Yoann Rodiere
 */
interface EntityIndexBinder {

	MutableEntityIndexBinding bind(IndexManagerGroupHolder holder, IndexedTypeIdentifier entityType, EntityIndexingInterceptor<?> interceptor, WorkerBuildContext buildContext);

	/**
	 * Controls how backends are identified.
	 * <p>
	 * This is primarily used to allow the dynamic sharding binder
	 * to only have one backend instance per backend name, shared among
	 * all shards, whereas the non-dynamic sharding binder will want
	 * one backend instance per shard (so as to allow multiple shards
	 * to have different types of backends, or differently configured backends).
	 *
	 * @param backendName The name of the backend, defining to implementation to use
	 * @param indexName The name of the index
	 * @return The backend identifier
	 */
	String createBackendIdentifier(String backendName, String indexName);

}
