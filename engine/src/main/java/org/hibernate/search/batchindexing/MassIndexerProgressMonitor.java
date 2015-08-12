/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
 */
public interface MassIndexerProgressMonitor extends IndexingMonitor {

	/**
	 * The number of Documents built;
	 * This is invoked several times and concurrently during
	 * the indexing process.
	 *
	 * @param number number of {@code Document}s built
	 */
	void documentsBuilt(int number);

	/**
	 * The number of entities loaded from database;
	 * This is invoked several times and concurrently during
	 * the indexing process.
	 *
	 * @param size number of entities loaded from database
	 */
	void entitiesLoaded(int size);

	/**
	 * The total count of entities to be indexed is
	 * added here; It could be called more than once,
	 * the implementation should add them up.
	 * This is invoked several times and concurrently during
	 * the indexing process.
	 *
	 * @param count number of newly indexed entities which has to
	 * be added to total count
	 */
	void addToTotalCount(long count);

	/**
	 * Invoked when the indexing is completed.
	 */
	void indexingCompleted();
}
