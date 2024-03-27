/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.massindexing.impl;

import java.util.concurrent.Future;

import org.hibernate.CacheMode;
import org.hibernate.search.MassIndexer;
import org.hibernate.search.batchindexing.MassIndexerProgressMonitor;
import org.hibernate.search.util.common.annotation.Incubating;

public class V5MigrationMassIndexerAdapter implements MassIndexer {

	private final org.hibernate.search.mapper.orm.massindexing.MassIndexer delegate;

	public V5MigrationMassIndexerAdapter(org.hibernate.search.mapper.orm.massindexing.MassIndexer delegate) {
		this.delegate = delegate;
	}

	@Override
	public MassIndexer typesToIndexInParallel(int threadsToIndexObjects) {
		delegate.typesToIndexInParallel( threadsToIndexObjects );
		return this;
	}

	@Override
	public MassIndexer threadsToLoadObjects(int numberOfThreads) {
		delegate.threadsToLoadObjects( numberOfThreads );
		return this;
	}

	@Override
	public MassIndexer batchSizeToLoadObjects(int batchSize) {
		delegate.batchSizeToLoadObjects( batchSize );
		return this;
	}

	@Override
	public MassIndexer cacheMode(CacheMode cacheMode) {
		delegate.cacheMode( cacheMode );
		return this;
	}

	@Override
	public MassIndexer optimizeOnFinish(boolean optimize) {
		delegate.mergeSegmentsOnFinish( optimize );
		return this;
	}

	@Override
	public MassIndexer optimizeAfterPurge(boolean optimize) {
		delegate.mergeSegmentsAfterPurge( optimize );
		return this;
	}

	@Override
	public MassIndexer purgeAllOnStart(boolean purgeAll) {
		delegate.purgeAllOnStart( purgeAll );
		return this;
	}

	@Override
	@Incubating
	public MassIndexer limitIndexedObjectsTo(long maximum) {
		delegate.limitIndexedObjectsTo( maximum );
		return this;
	}

	@Override
	public Future<?> start() {
		return delegate.start().toCompletableFuture();
	}

	@Override
	public void startAndWait() throws InterruptedException {
		delegate.startAndWait();
	}

	@Override
	public MassIndexer idFetchSize(int idFetchSize) {
		delegate.idFetchSize( idFetchSize );
		return this;
	}

	@Override
	public MassIndexer transactionTimeout(int timeoutInSeconds) {
		delegate.transactionTimeout( timeoutInSeconds );
		return this;
	}

	@Override
	public MassIndexer progressMonitor(MassIndexerProgressMonitor monitor) {
		delegate.monitor( new V5MigrationMassIndexerProgressMonitorAdapter( monitor ) );
		return this;
	}

	@Override
	public MassIndexer threadsForSubsequentFetching(int numberOfThreads) {
		return this;
	}
}
