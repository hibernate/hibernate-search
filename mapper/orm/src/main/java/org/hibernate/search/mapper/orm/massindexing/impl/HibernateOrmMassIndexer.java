/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.massindexing.impl;

import java.util.Set;
import java.util.concurrent.CompletionStage;
import org.hibernate.CacheMode;
import org.hibernate.search.engine.backend.session.spi.DetachedBackendSessionContext;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.schema.management.spi.PojoScopeSchemaManager;
import org.hibernate.search.mapper.pojo.work.spi.PojoScopeWorkspace;
import org.hibernate.search.mapper.pojo.massindexing.spi.MassIndexingMappingContext;
import org.hibernate.search.mapper.orm.massindexing.MassIndexer;
import org.hibernate.search.mapper.pojo.massindexing.MassIndexingFailureHandler;
import org.hibernate.search.mapper.pojo.massindexing.MassIndexingMonitor;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoDefaultMassIndexer;
import org.hibernate.search.mapper.pojo.massindexing.spi.MassIndexingContext;
import org.hibernate.search.mapper.orm.loading.impl.HibernateOrmMassIndexingOptions;
import org.hibernate.search.mapper.pojo.massindexing.loader.MassIndexingOptions;

public class HibernateOrmMassIndexer implements MassIndexer, MassIndexingOptions, HibernateOrmMassIndexingOptions {
	static final String THREAD_NAME_PREFIX = "Mass indexing - ";

	private Integer idLoadingTransactionTimeout;
	private CacheMode cacheMode;
	private final PojoDefaultMassIndexer delegate;

	public HibernateOrmMassIndexer(
			MassIndexingContext<?> massIndexingContext,
			MassIndexingMappingContext mappingContext,
			DetachedBackendSessionContext sessionContext,
			Set<? extends PojoRawTypeIdentifier<?>> targetedIndexedTypes,
			PojoScopeSchemaManager scopeSchemaManager,
			PojoScopeWorkspace scopeWorkspace) {
		delegate = new PojoDefaultMassIndexer( this,
				massIndexingContext, mappingContext, sessionContext,
				targetedIndexedTypes, scopeSchemaManager, scopeWorkspace );
	}

	@Override
	public String threadNamePrefix() {
		return THREAD_NAME_PREFIX;
	}

	@Override
	public String tenantIdentifier() {
		return delegate.tenantIdentifier();
	}

	@Override
	public int batchSize() {
		return delegate.batchSize();
	}

	@Override
	public long objectsLimit() {
		return delegate.objectsLimit();
	}

	@Override
	public int fetchSize() {
		return delegate.fetchSize();
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
	public MassIndexer batchSizeToLoadObjects(int batchSize) {
		delegate.batchSizeToLoadObjects( batchSize );
		return this;
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
	public MassIndexer limitIndexedObjectsTo(long maximum) {
		delegate.limitIndexedObjectsTo( maximum );
		return this;
	}

	@Override
	public CompletionStage start() {
		return delegate.start();
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
