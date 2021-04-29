/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.massindexing.impl;

import javax.transaction.TransactionManager;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder;
import org.hibernate.search.engine.backend.session.spi.DetachedBackendSessionContext;
import org.hibernate.search.mapper.orm.loading.impl.HibernateOrmEntityLoadingStrategy;
import org.hibernate.search.mapper.orm.loading.impl.HibernateOrmMassEntityLoader;
import org.hibernate.search.mapper.orm.loading.impl.HibernateOrmMassIdentifierLoader;
import org.hibernate.search.mapper.orm.loading.impl.HibernateOrmMassLoadingOptions;
import org.hibernate.search.mapper.orm.loading.impl.HibernateOrmQueryLoader;
import org.hibernate.search.mapper.orm.loading.impl.LoadingIndexedTypeContext;
import org.hibernate.search.mapper.orm.session.impl.HibernateOrmSessionTypeContextProvider;
import org.hibernate.search.mapper.pojo.loading.spi.PojoMassEntityLoader;
import org.hibernate.search.mapper.pojo.loading.spi.PojoMassEntitySink;
import org.hibernate.search.mapper.pojo.loading.spi.PojoMassIdentifierLoader;
import org.hibernate.search.mapper.pojo.loading.spi.PojoMassIdentifierSink;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexingContext;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexingEntityLoadingContext;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexingIdentifierLoadingContext;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexingLoadingStrategy;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.util.common.impl.SuppressingCloser;

public final class HibernateOrmMassIndexingContext
		implements PojoMassIndexingContext, HibernateOrmMassLoadingOptions {
	private final HibernateOrmMassIndexingMappingContext mappingContext;
	private final HibernateOrmSessionTypeContextProvider typeContextProvider;
	private final DetachedBackendSessionContext sessionContext;

	private CacheMode cacheMode;
	private Integer idLoadingTransactionTimeout;
	private int idFetchSize = 100; //reasonable default as we only load IDs
	private int objectLoadingBatchSize = 10;
	private long objectsLimit = 0; //means no limit at all

	public HibernateOrmMassIndexingContext(HibernateOrmMassIndexingMappingContext mappingContext,
			HibernateOrmSessionTypeContextProvider typeContextContainer,
			DetachedBackendSessionContext sessionContext) {
		this.mappingContext = mappingContext;
		this.typeContextProvider = typeContextContainer;
		this.sessionContext = sessionContext;
	}

	@Override
	public <T> PojoMassIndexingLoadingStrategy<? super T, ?> loadingStrategy(PojoRawTypeIdentifier<T> expectedType) {
		LoadingIndexedTypeContext<T> typeContext = typeContextProvider.indexedForExactType( expectedType );
		return new HibernateOrmMassIndexingLoadingStrategy<>( typeContext.loadingStrategy() );
	}

	public void idLoadingTransactionTimeout(int timeoutInSeconds) {
		this.idLoadingTransactionTimeout = timeoutInSeconds;
	}

	@Override
	public Integer idLoadingTransactionTimeout() {
		return idLoadingTransactionTimeout;
	}

	public void cacheMode(CacheMode cacheMode) {
		this.cacheMode = cacheMode;
	}

	@Override
	public CacheMode cacheMode() {
		return cacheMode;
	}

	public void objectLoadingBatchSize(int batchSize) {
		if ( batchSize < 1 ) {
			throw new IllegalArgumentException( "batchSize must be at least 1" );
		}
		this.objectLoadingBatchSize = batchSize;
	}

	@Override
	public int objectLoadingBatchSize() {
		return objectLoadingBatchSize;
	}

	public void objectsLimit(long maximum) {
		this.objectsLimit = maximum;
	}

	@Override
	public long objectsLimit() {
		return objectsLimit;
	}

	public void idFetchSize(int idFetchSize) {
		// don't check for positive/zero values as it's actually used by some databases
		// as special values which might be useful.
		this.idFetchSize = idFetchSize;
	}

	@Override
	public int idFetchSize() {
		return idFetchSize;
	}

	private static TransactionManager lookupTransactionManager(SessionFactoryImplementor sessionFactory) {
		return sessionFactory.getServiceRegistry()
				.getService( JtaPlatform.class )
				.retrieveTransactionManager();
	}

	private static TransactionCoordinatorBuilder lookupTransactionCoordinatorBuilder(
			SessionFactoryImplementor sessionFactory) {
		return sessionFactory.getServiceRegistry().getService( TransactionCoordinatorBuilder.class );
	}

	private final class HibernateOrmMassIndexingLoadingStrategy<E, I> implements PojoMassIndexingLoadingStrategy<E, I> {

		private final HibernateOrmEntityLoadingStrategy<E, I> delegate;

		public HibernateOrmMassIndexingLoadingStrategy(HibernateOrmEntityLoadingStrategy<E, I> delegate) {
			this.delegate = delegate;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			HibernateOrmMassIndexingLoadingStrategy<?, ?> that = (HibernateOrmMassIndexingLoadingStrategy<?, ?>) o;
			return delegate.equals( that.delegate );
		}

		@Override
		public int hashCode() {
			return delegate.hashCode();
		}

		@Override
		public PojoMassIdentifierLoader createIdentifierLoader(PojoMassIndexingIdentifierLoadingContext<E, I> context) {
			SessionFactoryImplementor sessionFactory = mappingContext.sessionFactory();
			TransactionManager transactionManager = lookupTransactionManager( sessionFactory );
			TransactionCoordinatorBuilder transactionCoordinatorBuilder = lookupTransactionCoordinatorBuilder(
					sessionFactory );
			HibernateOrmQueryLoader<E, I> typeQueryLoader = delegate.createQueryLoader( context.includedTypes() );
			SharedSessionContractImplementor session = (SharedSessionContractImplementor) sessionFactory
					.withStatelessOptions()
					.tenantIdentifier( sessionContext.tenantIdentifier() )
					.openStatelessSession();
			try {
				PojoMassIdentifierSink<I> sink = context.createSink();
				return new HibernateOrmMassIdentifierLoader<>( transactionManager, transactionCoordinatorBuilder,
						typeQueryLoader, HibernateOrmMassIndexingContext.this, sink, session
				);
			}
			catch (RuntimeException e) {
				new SuppressingCloser( e ).push( SharedSessionContractImplementor::close, session );
				throw e;
			}
		}

		@Override
		public PojoMassEntityLoader<I> createEntityLoader(PojoMassIndexingEntityLoadingContext<E> context) {
			SessionFactoryImplementor sessionFactory = mappingContext.sessionFactory();
			TransactionManager transactionManager = lookupTransactionManager( sessionFactory );
			HibernateOrmQueryLoader<E, ?> typeQueryLoader = delegate.createQueryLoader( context.includedTypes() );
			SessionImplementor session = (SessionImplementor) sessionFactory
					.withOptions()
					.tenantIdentifier( sessionContext.tenantIdentifier() )
					.openSession();
			try {
				session.setHibernateFlushMode( FlushMode.MANUAL );
				session.setCacheMode( cacheMode() );
				session.setDefaultReadOnly( true );

				PojoMassEntitySink<E> sink = context.createSink( mappingContext.sessionContext( session ) );
				return new HibernateOrmMassEntityLoader<>( transactionManager, typeQueryLoader,
						HibernateOrmMassIndexingContext.this, sink, session );
			}
			catch (RuntimeException e) {
				new SuppressingCloser( e ).push( SessionImplementor::close, session );
				throw e;
			}
		}

	}
}
