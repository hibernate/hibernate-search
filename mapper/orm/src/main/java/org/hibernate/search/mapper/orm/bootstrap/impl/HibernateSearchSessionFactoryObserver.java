/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.bootstrap.impl;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.search.mapper.orm.event.impl.HibernateSearchEventListener;
import org.hibernate.search.mapper.orm.impl.HibernateSearchContextService;
import org.hibernate.search.mapper.orm.spi.EnvironmentSynchronizer;
import org.hibernate.search.util.common.impl.Closer;

/**
 * A {@code SessionFactoryObserver} registered with Hibernate ORM during the integration phase.
 * This observer will initialize Hibernate Search once the {@code SessionFactory} is built.
 *
 * @author Hardy Ferentschik
 * @see HibernateSearchIntegrator
 */
public class HibernateSearchSessionFactoryObserver implements SessionFactoryObserver {

	private final HibernateOrmIntegrationBooterImpl booter;
	private final HibernateSearchEventListener listener;

	private final CompletableFuture<HibernateSearchContextService> contextFuture = new CompletableFuture<>();
	private final CompletableFuture<?> closingTrigger = new CompletableFuture<>();


	//Guarded by synchronization on this
	// TODO JMX
//	private JMXHook jmx;

	HibernateSearchSessionFactoryObserver(
			HibernateOrmIntegrationBooterImpl booter,
			HibernateSearchEventListener listener) {
		this.booter = booter;
		this.listener = listener;

		/*
		 * Make sure that if a Search integrator is created, it will eventually get closed,
		 * either when the environment is destroyed (see the use of EnvironmentSynchronizer in #sessionFactoryCreated)
		 * or when the session factory is closed (see #sessionFactoryClosed),
		 * whichever happens first.
		 */
		contextFuture.thenAcceptBoth( closingTrigger,
				(context, ignored) -> this.cleanup( context ) );
	}

	@Override
	public void sessionFactoryCreated(SessionFactory factory) {
		boolean failedBootScheduling = true;
		try {
			SessionFactoryImplementor sessionFactoryImplementor = (SessionFactoryImplementor) factory;
			listener.initialize( contextFuture );

			Optional<EnvironmentSynchronizer> environmentSynchronizer = booter.getEnvironmentSynchronizer();
			if ( environmentSynchronizer.isPresent() ) {
				environmentSynchronizer.get().whenEnvironmentDestroying( () -> {
					// Trigger integrator closing if the integrator actually exists and wasn't already closed
					closingTrigger.complete( null );
				} );
				environmentSynchronizer.get().whenEnvironmentReady( () -> boot( sessionFactoryImplementor ) );
			}
			else {
				boot( sessionFactoryImplementor );
			}

			failedBootScheduling = false;
		}
		finally {
			if ( failedBootScheduling ) {
				cancelBoot();
			}
		}
	}

	/**
	 * Boot Hibernate Search if it hasn't booted already,
	 * and complete {@link #contextFuture}.
	 * <p>
	 * This method is synchronized in order to avoid booting Hibernate Search
	 * after (or while) the boot has been canceled.
	 *
	 * @param sessionFactoryImplementor The factory on which to graft Hibernate Search.
	 *
	 * @see #cancelBoot()
	 */
	private synchronized void boot(SessionFactoryImplementor sessionFactoryImplementor) {
		if ( contextFuture.isDone() ) {
			return;
		}
		try {
			HibernateSearchContextService contextService = booter.boot( sessionFactoryImplementor );
			contextFuture.complete( contextService );
		}
		catch (RuntimeException e) {
			contextFuture.completeExceptionally( e );
			// This will make the SessionFactory abort and close itself
			throw e;
		}
	}

	@Override
	public synchronized void sessionFactoryClosing(SessionFactory factory) {
		cancelBoot();
	}

	/**
	 * Cancel the planned boot if it hasn't happened already.
	 * <p>
	 * This method is synchronized in order to avoid canceling the boot while it is ongoing,
	 * which could lead to resource leaks.
	 *
	 * @see #boot(SessionFactoryImplementor)
	 */
	private synchronized void cancelBoot() {
		contextFuture.cancel( false );
	}

	@Override
	public void sessionFactoryClosed(SessionFactory factory) {
		/*
		 * Trigger integrator closing if the integrator actually exists and wasn't already closed
		 * The closing might have been triggered already if an EnvironmentSynchronizer is being used
		 * (see #sessionFactoryCreated).
		 */
		closingTrigger.complete( null );
	}

	private synchronized void cleanup(HibernateSearchContextService context) {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.push( c -> c.getIntegration().close(), context );
			// TODO JMX
			// closer.push( JMXHook::unRegisterIfRegistered, jmx );
		}
	}

}

