/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.massindexing.impl;

import javax.transaction.TransactionManager;

import org.hibernate.FlushMode;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder;
import org.hibernate.search.mapper.orm.loading.impl.HibernateOrmEntityLoadingStrategy;
import org.hibernate.search.mapper.orm.loading.impl.HibernateOrmMassEntityLoader;
import org.hibernate.search.mapper.orm.loading.impl.HibernateOrmMassIdentifierLoader;
import org.hibernate.search.mapper.orm.loading.impl.HibernateOrmMassIndexingOptions;
import org.hibernate.search.mapper.orm.loading.impl.HibernateOrmQueryLoader;
import org.hibernate.search.mapper.pojo.loading.spi.PojoMassIdentifierLoader;
import org.hibernate.search.mapper.pojo.loading.spi.PojoMassIdentifierSink;
import org.hibernate.search.mapper.pojo.loading.spi.PojoMassEntityLoader;
import org.hibernate.search.mapper.pojo.loading.spi.PojoMassEntitySink;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexingIdentifierLoadingContext;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexingEntityLoadingContext;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexingLoadingStrategy;
import org.hibernate.search.util.common.impl.SuppressingCloser;

class HibernateOrmMassIndexingLoadingStrategy<E, I>
		implements PojoMassIndexingLoadingStrategy<E, I, HibernateOrmMassIndexingOptions> {

	private final HibernateOrmMassIndexingMappingContext mappingContext;
	private final HibernateOrmEntityLoadingStrategy<E, I> delegate;

	public HibernateOrmMassIndexingLoadingStrategy(HibernateOrmMassIndexingMappingContext mappingContext,
			HibernateOrmEntityLoadingStrategy<E, I> delegate) {
		this.mappingContext = mappingContext;
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
	public PojoMassIdentifierLoader createIdentifierLoader(
			PojoMassIndexingIdentifierLoadingContext<E, I> context, HibernateOrmMassIndexingOptions options) {
		SessionFactoryImplementor sessionFactory = mappingContext.sessionFactory();
		TransactionManager transactionManager = lookupTransactionManager( sessionFactory );
		TransactionCoordinatorBuilder transactionCoordinatorBuilder = lookupTransactionCoordinatorBuilder(
				sessionFactory );
		HibernateOrmQueryLoader<E, I> typeQueryLoader = delegate.createQueryLoader( context.includedTypes() );
		SharedSessionContractImplementor session = (SharedSessionContractImplementor) sessionFactory
				.withStatelessOptions()
				.tenantIdentifier( options.tenantIdentifier() )
				.openStatelessSession();
		try {
			PojoMassIdentifierSink<I> sink = context.createSink();
			return new HibernateOrmMassIdentifierLoader<>( transactionManager, transactionCoordinatorBuilder,
					typeQueryLoader, options, sink, session
			);
		}
		catch (RuntimeException e) {
			new SuppressingCloser( e ).push( SharedSessionContractImplementor::close, session );
			throw e;
		}
	}

	@Override
	public PojoMassEntityLoader<I> createEntityLoader(PojoMassIndexingEntityLoadingContext<E> context,
			HibernateOrmMassIndexingOptions options) {
		SessionFactoryImplementor sessionFactory = mappingContext.sessionFactory();
		TransactionManager transactionManager = lookupTransactionManager( sessionFactory );
		HibernateOrmQueryLoader<E, ?> typeQueryLoader = delegate.createQueryLoader( context.includedTypes() );
		SessionImplementor session = (SessionImplementor) sessionFactory
				.withOptions()
				.tenantIdentifier( options.tenantIdentifier() )
				.openSession();
		try {
			session.setHibernateFlushMode( FlushMode.MANUAL );
			session.setCacheMode( options.cacheMode() );
			session.setDefaultReadOnly( true );

			PojoMassEntitySink<E> sink = context.createSink( mappingContext.sessionContext( session ) );
			return new HibernateOrmMassEntityLoader<>( transactionManager, typeQueryLoader, options, sink, session );
		}
		catch (RuntimeException e) {
			new SuppressingCloser( e ).push( SessionImplementor::close, session );
			throw e;
		}
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

}
