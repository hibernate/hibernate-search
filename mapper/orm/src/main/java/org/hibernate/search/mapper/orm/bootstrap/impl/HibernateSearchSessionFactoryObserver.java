/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.bootstrap.impl;

import java.util.concurrent.CompletableFuture;

import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.search.util.common.impl.Futures;

/**
 * A {@code SessionFactoryObserver} registered with Hibernate ORM during the integration phase.
 * This observer will initialize Hibernate Search once the {@code SessionFactory} is built.
 *
 * @author Hardy Ferentschik
 * @see HibernateSearchIntegrator
 */
class HibernateSearchSessionFactoryObserver implements SessionFactoryObserver {

	private final CompletableFuture<?> contextFuture;
	private final CompletableFuture<SessionFactoryImplementor> sessionFactoryCreatedFuture;
	private final CompletableFuture<?> sessionFactoryClosingFuture;

	HibernateSearchSessionFactoryObserver(
			CompletableFuture<?> contextFuture,
			CompletableFuture<SessionFactoryImplementor> sessionFactoryCreatedFuture,
			CompletableFuture<?> sessionFactoryClosingFuture) {
		this.contextFuture = contextFuture;
		this.sessionFactoryCreatedFuture = sessionFactoryCreatedFuture;
		this.sessionFactoryClosingFuture = sessionFactoryClosingFuture;
	}

	@Override
	public synchronized void sessionFactoryCreated(SessionFactory factory) {
		SessionFactoryImplementor sessionFactoryImplementor = (SessionFactoryImplementor) factory;
		sessionFactoryCreatedFuture.complete( sessionFactoryImplementor );
		// If the above triggered bootstrap and it failed, propagate the exception
		if ( contextFuture.isCompletedExceptionally() ) {
			Futures.unwrappedExceptionJoin( contextFuture );
		}
	}

	@Override
	public synchronized void sessionFactoryClosing(SessionFactory factory) {
		sessionFactoryClosingFuture.complete( null );
		// If the above triggered shutdown and it failed, the exception will be logged.
	}

}

