/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.massindexing.impl;

import java.util.concurrent.CompletionStage;

import org.hibernate.CacheMode;
import org.hibernate.search.engine.backend.session.spi.DetachedBackendSessionContext;
import org.hibernate.search.mapper.orm.loading.impl.HibernateOrmMassIndexingOptions;
import org.hibernate.search.mapper.orm.massindexing.MassIndexer;
import org.hibernate.search.mapper.pojo.massindexing.MassIndexingFailureHandler;
import org.hibernate.search.mapper.pojo.massindexing.MassIndexingMonitor;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexer;

public class HibernateOrmMassIndexer implements MassIndexer, HibernateOrmMassIndexingOptions {

	private final PojoMassIndexer<HibernateOrmMassIndexingOptions> delegate;
	private final DetachedBackendSessionContext sessionContext;

	private CacheMode cacheMode;
	private Integer idLoadingTransactionTimeout;
	private int idFetchSize = 100; //reasonable default as we only load IDs
	private int objectLoadingBatchSize = 10;
	private long objectsLimit = 0; //means no limit at all

	public HibernateOrmMassIndexer(PojoMassIndexer<HibernateOrmMassIndexingOptions> delegate,
			DetachedBackendSessionContext sessionContext) {
		this.delegate = delegate;
		this.sessionContext = sessionContext;
	}

	@Override
	public String tenantIdentifier() {
		return sessionContext.tenantIdentifier();
	}

	@Override
	public MassIndexer transactionTimeout(int timeoutInSeconds) {
		this.idLoadingTransactionTimeout = timeoutInSeconds;
		return this;
	}

	@Override
	public Integer transactionTimeout() {
		return idLoadingTransactionTimeout;
	}

	@Override
	public MassIndexer cacheMode(CacheMode cacheMode) {
		this.cacheMode = cacheMode;
		return this;
	}

	@Override
	public CacheMode cacheMode() {
		return cacheMode;
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
	public HibernateOrmMassIndexer batchSizeToLoadObjects(int batchSize) {
		if ( batchSize < 1 ) {
			throw new IllegalArgumentException( "batchSize must be at least 1" );
		}
		this.objectLoadingBatchSize = batchSize;
		return this;
	}

	@Override
	public int batchSizeToLoadObjects() {
		return objectLoadingBatchSize;
	}

	@Override
	public MassIndexer mergeSegmentsOnFinish(boolean enable) {
		delegate.mergeSegmentsOnFinish( enable );
		return this;
	}

	@Override
	public MassIndexer mergeSegmentsAfterPurge(boolean enable) {
		delegate.mergeSegmentsAfterPurge( enable );
		return this;
	}

	@Override
	public MassIndexer dropAndCreateSchemaOnStart(boolean dropAndCreateSchema) {
		delegate.dropAndCreateSchemaOnStart( dropAndCreateSchema );
		return this;
	}

	@Override
	public MassIndexer purgeAllOnStart(boolean purgeAll) {
		delegate.purgeAllOnStart( purgeAll );
		return this;
	}

	@Override
	public HibernateOrmMassIndexer limitIndexedObjectsTo(long maximum) {
		this.objectsLimit = maximum;
		return this;
	}

	@Override
	public long objectsLimit() {
		return objectsLimit;
	}

	@Override
	public CompletionStage start() {
		return delegate.start( this );
	}

	@Override
	public void startAndWait() throws InterruptedException {
		delegate.startAndWait( this );
	}

	@Override
	public HibernateOrmMassIndexer idFetchSize(int idFetchSize) {
		// don't check for positive/zero values as it's actually used by some databases
		// as special values which might be useful.
		this.idFetchSize = idFetchSize;
		return this;
	}

	@Override
	public int idFetchSize() {
		return idFetchSize;
	}

	@Override
	public MassIndexer monitor(MassIndexingMonitor monitor) {
		delegate.monitor( monitor );
		return this;
	}

	@Override
	public MassIndexer failureHandler(MassIndexingFailureHandler failureHandler) {
		delegate.failureHandler( failureHandler );
		return this;
	}

}
