/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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

	private final CompletableFuture<SessionFactoryImplementor> sessionFactoryCreatedFuture;
	private final CompletableFuture<?> sessionFactoryClosingFuture;
	private final CompletableFuture<?> contextFuture;

	HibernateSearchSessionFactoryObserver(
			CompletableFuture<SessionFactoryImplementor> sessionFactoryCreatedFuture,
			CompletableFuture<?> sessionFactoryClosingFuture,
			CompletableFuture<?> contextFuture) {
		this.sessionFactoryCreatedFuture = sessionFactoryCreatedFuture;
		this.sessionFactoryClosingFuture = sessionFactoryClosingFuture;
		this.contextFuture = contextFuture;
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
	}

}

