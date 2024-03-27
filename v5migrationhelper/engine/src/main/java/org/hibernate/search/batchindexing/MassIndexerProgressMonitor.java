/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.batchindexing;

import org.hibernate.search.backend.IndexingMonitor;

/**
 * As a MassIndexer can take some time to finish it's job,
 * a MassIndexerProgressMonitor can be defined in the configuration
 * property hibernate.search.worker.indexing.monitor
 * implementing this interface to track indexing performance.
 * <p>
 * Implementations must:
 * <ul>
 * <li>	be threadsafe </li>
 * <li> have a no-arg constructor </li>
 * </ul>
 *
 * @author Sanne Grinovero
 * @author Hardy Ferentschik
 * @deprecated Instead of using Hibernate Search 5 APIs, get a {@code org.hibernate.search.mapper.orm.session.SearchSession}
 * using {@code org.hibernate.search.mapper.orm.Search#session(org.hibernate.Session)},
 * create a mass indexer with {@code org.hibernate.search.mapper.orm.session.SearchSession#massIndexer(Class[])},
 * and implement the interface {@code org.hibernate.search.mapper.orm.massindexing.MassIndexingMonitor}
 * in your monitor.
 * Refer to the <a href="https://hibernate.org/search/documentation/migrate/6.0/">migration guide</a> for more information.
 */
@Deprecated
public interface MassIndexerProgressMonitor extends IndexingMonitor {

	/**
	 * Notify the monitor that {@code increment} more documents have been built.
	 * <p>
	 * Summing the numbers passed to this method gives the total
	 * number of documents that have been built so far.
	 * <p>
	 * This method is invoked several times during indexing,
	 * and calls are <strong>incremental</strong>:
	 * calling {@code documentsBuilt(3)} and then {@code documentsBuilt(1)}
	 * should be understood as "3+1 documents, i.e. 4 documents have been built".
	 * <p>
	 * This method can be invoked from several threads thus implementors are required to be thread-safe.
	 *
	 * @param increment additional number of documents built
	 */
	void documentsBuilt(int increment);

	/**
	 * Notify the monitor that {@code increment} more entities have been loaded from the database.
	 * <p>
	 * Summing the numbers passed to this method gives the total
	 * number of entities that have been loaded so far.
	 * <p>
	 * This method is invoked several times during indexing,
	 * and calls are <strong>incremental</strong>:
	 * calling {@code entitiesLoaded(3)} and then {@code entitiesLoaded(1)}
	 * should be understood as "3+1 documents, i.e. 4 documents have been loaded".
	 * <p>
	 * This method can be invoked from several threads thus implementors are required to be thread-safe.
	 *
	 * @param increment additional number of entities loaded from database
	 */
	void entitiesLoaded(int increment);

	/**
	 * Notify the monitor that {@code increment} more entities have been
	 * detected in the database and will be indexed.
	 * <p>
	 * Summing the numbers passed to this method gives the total
	 * number of entities that Hibernate Search plans to index.
	 * This number can be incremented during indexing
	 * as Hibernate Search moves from one entity type to the next.
	 * <p>
	 * This method is invoked several times during indexing,
	 * and calls are <strong>incremental</strong>:
	 * calling {@code addToTotalCount(3)} and then {@code addToTotalCount(1)}
	 * should be understood as "3+1 documents, i.e. 4 documents will be indexed".
	 * <p>
	 * This method can be invoked from several threads thus implementors are required to be thread-safe.
	 *
	 * @param increment additional number of entities that will be indexed
	 */
	void addToTotalCount(long increment);

	/**
	 * Notify the monitor that indexing is complete.
	 */
	void indexingCompleted();
}
