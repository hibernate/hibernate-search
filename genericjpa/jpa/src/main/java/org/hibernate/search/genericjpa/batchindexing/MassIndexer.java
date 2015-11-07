/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.genericjpa.batchindexing;

import java.util.concurrent.Future;

import org.hibernate.search.genericjpa.entity.EntityProvider;

/**
 * @author Martin Braun
 */
public interface MassIndexer {

	/**
	 * set whether to purge all entities in the index on start
	 */
	MassIndexer purgeAllOnStart(boolean purgeAllOnStart);

	/**
	 * set whether to optimize after purging on start
	 */
	MassIndexer optimizeAfterPurge(boolean optimizeAfterPurge);

	/**
	 * set whether to optimize in the end
	 */
	MassIndexer optimizeOnFinish(boolean optimizeOnFinish);

	/**
	 * set the batchsize to be used to load the ids from the database. this is usually a higher value than the batchsize
	 * for the objects
	 */
	MassIndexer batchSizeToLoadIds(int batchSizeToLoadIds);

	/**
	 * set the batchsize to be used to load the objects from the database. this is usually a lower value than the
	 * batchsize for the ids
	 */
	MassIndexer batchSizeToLoadObjects(int batchSizeToLoadObjects);

	/**
	 * set the amount of threads to be used for loading the ids. this should not be set too high
	 */
	MassIndexer threadsToLoadIds(int threadsToLoadIds);

	/**
	 * set the amount of threads to be used for loading the objects from the database. this value is generally set
	 * higher than the threads used to load the ids
	 */
	MassIndexer threadsToLoadObjects(int threadsToLoadObjects);

	MassIndexer progressMonitor(MassIndexerProgressMonitor progressMonitor);

	/**
	 * custom fetching for the objects can be implemented here
	 */
	MassIndexer entityProvider(EntityProvider entityProvider);

	/**
	 * only applied in a JTA context
	 */
	MassIndexer idProducerTransactionTimeout(int seconds);

	/**
	 * starts the process and doesn't wait for completion
	 */
	Future<?> start();

	/**
	 * starts the process and waits for completion
	 */
	void startAndWait() throws InterruptedException;

}
