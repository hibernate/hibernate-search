/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.loading.impl;

import java.util.List;
import javax.persistence.LockModeType;

import org.hibernate.FlushMode;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.query.Query;
import org.hibernate.search.mapper.orm.common.impl.TransactionHelper;
import org.hibernate.search.mapper.pojo.loading.spi.PojoMassEntityLoader;
import org.hibernate.search.mapper.pojo.loading.spi.PojoMassEntitySink;

public final class HibernateOrmMassEntityLoader<E, I> implements PojoMassEntityLoader<I> {

	private static final String ID_PARAMETER_NAME = "ids";

	private final HibernateOrmQueryLoader<E, ?> typeQueryLoader;
	private final HibernateOrmMassLoadingOptions options;
	private final PojoMassEntitySink<E> sink;
	private final SessionImplementor session;
	private final TransactionHelper transactionHelper;

	public HibernateOrmMassEntityLoader(HibernateOrmQueryLoader<E, ?> typeGroupLoader,
			HibernateOrmMassLoadingOptions options,
			PojoMassEntitySink<E> sink,
			SessionImplementor session) {
		this.typeQueryLoader = typeGroupLoader;
		this.options = options;
		this.sink = sink;
		this.session = session;
		this.transactionHelper = new TransactionHelper( session .getSessionFactory() );
	}

	@Override
	public void close() {
		session.close();
	}

	@Override
	public void load(List<I> identifiers) {
		transactionHelper.begin( session, null );
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
				transactionHelper.rollback( session );
			}
			catch (RuntimeException e2) {
				e.addSuppressed( e2 );
			}
			throw e;
		}
		transactionHelper.commit( session );
	}
}
