/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jpa.impl;

import java.util.concurrent.Future;

import org.hibernate.CacheMode;
import org.hibernate.search.batchindexing.MassIndexerProgressMonitor;
import org.hibernate.search.jpa.MassIndexer;

/**
 * Temporarily in place. Later, the ORMMassIndexerAdapter will have an abstraction layer so it can be used with genericJPA and ORM at once
 * <p>
 * Created by Martin on 27.11.2015.
 */
public class ORMMassIndexerAdapter implements MassIndexer {

	public ORMMassIndexerAdapter(org.hibernate.search.MassIndexer ormMassIndexer) {
		this.ormMassIndexer = ormMassIndexer;
	}

	private final org.hibernate.search.MassIndexer ormMassIndexer;

	public ORMMassIndexerAdapter cacheMode(CacheMode cacheMode) {
		ormMassIndexer.cacheMode( cacheMode );
		return this;
	}

	@Override
	public ORMMassIndexerAdapter typesToIndexInParallel(int threadsToIndexObjects) {
		ormMassIndexer.typesToIndexInParallel( threadsToIndexObjects );
		return this;
	}

	@Override
	public ORMMassIndexerAdapter threadsToLoadObjects(int numberOfThreads) {
		ormMassIndexer.threadsToLoadObjects( numberOfThreads );
		return this;
	}

	@Override
	public ORMMassIndexerAdapter batchSizeToLoadObjects(int batchSize) {
		ormMassIndexer.batchSizeToLoadObjects( batchSize );
		return this;
	}

	@Override
	@Deprecated
	public ORMMassIndexerAdapter threadsForSubsequentFetching(int numberOfThreads) {
		ormMassIndexer.threadsForSubsequentFetching( numberOfThreads );
		return this;
	}

	@Override
	public ORMMassIndexerAdapter progressMonitor(MassIndexerProgressMonitor monitor) {
		ormMassIndexer.progressMonitor( monitor );
		return this;
	}

	@Override
	public ORMMassIndexerAdapter optimizeOnFinish(boolean optimize) {
		ormMassIndexer.optimizeOnFinish( optimize );
		return this;
	}

	@Override
	public ORMMassIndexerAdapter optimizeAfterPurge(boolean optimize) {
		ormMassIndexer.optimizeAfterPurge( optimize );
		return this;
	}

	@Override
	public ORMMassIndexerAdapter purgeAllOnStart(boolean purgeAll) {
		ormMassIndexer.purgeAllOnStart( purgeAll );
		return this;
	}

	@Override
	public ORMMassIndexerAdapter limitIndexedObjectsTo(long maximum) {
		ormMassIndexer.limitIndexedObjectsTo( maximum );
		return this;
	}

	@Override
	public Future<?> start() {
		return ormMassIndexer.start();
	}

	@Override
	public void startAndWait() throws InterruptedException {
		ormMassIndexer.startAndWait();
	}

	@Override
	public ORMMassIndexerAdapter idFetchSize(int idFetchSize) {
		ormMassIndexer.idFetchSize( idFetchSize );
		return this;
	}

	@Override
	public ORMMassIndexerAdapter transactionTimeout(int timeoutInSeconds) {
		ormMassIndexer.transactionTimeout( timeoutInSeconds );
		return this;
	}
}
