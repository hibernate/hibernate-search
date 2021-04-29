/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.loading.impl;

import java.lang.invoke.MethodHandles;
import java.util.List;
import javax.persistence.LockModeType;
import javax.transaction.TransactionManager;

import org.hibernate.FlushMode;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.query.Query;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.mapper.pojo.loading.spi.PojoMassEntityLoader;
import org.hibernate.search.mapper.pojo.loading.spi.PojoMassEntitySink;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public final class HibernateOrmMassEntityLoader<E, I> implements PojoMassEntityLoader<I> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final String ID_PARAMETER_NAME = "ids";

	private final TransactionManager transactionManager;
	private final HibernateOrmQueryLoader<E, ?> typeQueryLoader;
	private final HibernateOrmMassLoadingOptions options;
	private final PojoMassEntitySink<E> sink;
	private final SessionImplementor session;

	public HibernateOrmMassEntityLoader(TransactionManager transactionManager,
			HibernateOrmQueryLoader<E, ?> typeGroupLoader,
			HibernateOrmMassLoadingOptions options,
			PojoMassEntitySink<E> sink,
			SessionImplementor session) {
		this.transactionManager = transactionManager;
		this.typeQueryLoader = typeGroupLoader;
		this.options = options;
		this.sink = sink;
		this.session = session;
	}

	@Override
	public void close() {
		session.close();
	}

	@Override
	public void load(List<I> identifiers) {
		beginTransaction( options.idLoadingTransactionTimeout() );
		try {
			Query<E> query = typeQueryLoader.createLoadingQuery( session, ID_PARAMETER_NAME )
					.setParameter( ID_PARAMETER_NAME, identifiers )
					.setCacheMode( options.cacheMode() )
					.setLockMode( LockModeType.NONE )
					.setCacheable( false )
					.setHibernateFlushMode( FlushMode.MANUAL )
					.setFetchSize( identifiers.size() );
			sink.accept( query.getResultList() );
			session.clear();
		}
		catch (Exception e) {
			try {
				rollbackTransaction();
			}
			catch (Exception e2) {
				e.addSuppressed( e2 );
			}
			throw e;
		}
		commitTransaction();
	}

	private void beginTransaction(Integer transactionTimeout) {
		try {
			if ( transactionManager != null ) {
				if ( transactionTimeout != null ) {
					transactionManager.setTransactionTimeout( transactionTimeout );
				}
				transactionManager.begin();
			}
			else {
				session.beginTransaction();
			}
		}
		catch (Exception e) {
			throw log.massIndexingTransactionHandlingException( e.getMessage(), e );
		}
	}

	private void commitTransaction() {
		try {
			if ( transactionManager != null ) {
				transactionManager.commit();
			}
			else {
				session.accessTransaction().commit();
			}
		}
		catch (Exception e) {
			throw log.massIndexingTransactionHandlingException( e.getMessage(), e );
		}
	}

	private void rollbackTransaction() {
		try {
			if ( transactionManager != null ) {
				transactionManager.rollback();
			}
			else {
				session.accessTransaction().rollback();
			}
		}
		catch (Exception e) {
			throw log.massIndexingTransactionHandlingException( e.getMessage(), e );
		}
	}
}
