/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.loading.impl;

import java.util.List;

import org.hibernate.FlushMode;
import org.hibernate.LockOptions;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.search.mapper.orm.common.spi.TransactionHelper;
import org.hibernate.search.mapper.orm.loading.spi.HibernateOrmQueryLoader;
import org.hibernate.search.mapper.pojo.loading.spi.PojoMassEntityLoader;
import org.hibernate.search.mapper.pojo.loading.spi.PojoMassEntitySink;

public final class HibernateOrmMassEntityLoader<E, I> implements PojoMassEntityLoader<I> {

	private static final String ID_PARAMETER_NAME = "ids";

	private final HibernateOrmQueryLoader<E, ?> typeQueryLoader;
	private final HibernateOrmMassLoadingContext options;
	private final PojoMassEntitySink<E> sink;
	private final SessionImplementor session;
	private final TransactionHelper transactionHelper;

	public HibernateOrmMassEntityLoader(HibernateOrmQueryLoader<E, ?> typeGroupLoader,
			HibernateOrmMassLoadingContext options,
			PojoMassEntitySink<E> sink,
			SessionImplementor session) {
		this.typeQueryLoader = typeGroupLoader;
		this.options = options;
		this.sink = sink;
		this.session = session;
		this.transactionHelper = new TransactionHelper( session.getSessionFactory(), null );
	}

	@Override
	public void close() {
		session.close();
	}

	@Override
	public void load(List<I> identifiers) throws InterruptedException {
		transactionHelper.begin( session );
		try {
			sink.accept( typeQueryLoader.uniquePropertyIsTheEntityId() ? multiLoad( identifiers ) : queryByIds( identifiers ) );
			session.clear();
		}
		catch (Exception e) {
			transactionHelper.rollbackSafely( session, e );
			throw e;
		}
		transactionHelper.commit( session );
	}

	private List<E> multiLoad(List<I> identifiers) {
		return typeQueryLoader.createMultiIdentifierLoadAccess( session )
				.with( options.cacheMode() )
				.with( LockOptions.NONE )
				.multiLoad( identifiers );
	}

	private List<E> queryByIds(List<I> identifiers) {
		return typeQueryLoader.createLoadingQuery( session, ID_PARAMETER_NAME )
				.setParameter( ID_PARAMETER_NAME, identifiers )
				.setCacheMode( options.cacheMode() )
				.setLockOptions( LockOptions.NONE )
				.setCacheable( false )
				.setHibernateFlushMode( FlushMode.MANUAL )
				.setFetchSize( identifiers.size() )
				.list();
	}
}
