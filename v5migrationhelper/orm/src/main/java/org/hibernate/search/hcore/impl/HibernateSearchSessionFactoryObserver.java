/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.hcore.impl;

import java.util.concurrent.CompletableFuture;

import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.jndi.spi.JndiService;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.search.cfg.impl.SearchConfigurationFromHibernateCore;
import org.hibernate.search.engine.Version;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.event.impl.FullTextIndexEventListener;
import org.hibernate.search.hcore.spi.EnvironmentSynchronizer;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.spi.SearchIntegratorBuilder;
import org.hibernate.search.util.impl.Closer;

/**
 * A {@code SessionFactoryObserver} registered with Hibernate ORM during the integration phase. This observer will
 * create the Search factory once the {@code SessionFactory} is built.
 *
 * @author Hardy Ferentschik
 * @see HibernateSearchIntegrator
 */
public class HibernateSearchSessionFactoryObserver implements SessionFactoryObserver {
	static {
		Version.touch();
	}

	private final ConfigurationService configurationService;
	private final JndiService namingService;
	private final ClassLoaderService classLoaderService;
	private final EnvironmentSynchronizer environmentSynchronizer;
	private final ManagedBeanRegistry managedBeanRegistry;
	private final FullTextIndexEventListener listener;
	private final Metadata metadata;

	private final CompletableFuture<ExtendedSearchIntegrator> extendedSearchIntegratorFuture = new CompletableFuture<>();
	private final CompletableFuture<?> extendedSearchIntegratorClosingTrigger = new CompletableFuture<>();

	//Guarded by synchronization on this
	private JMXHook jmx;

	public HibernateSearchSessionFactoryObserver(
			Metadata metadata,
			ConfigurationService configurationService,
			FullTextIndexEventListener listener,
			ClassLoaderService classLoaderService,
			EnvironmentSynchronizer environmentSynchronizer,
			ManagedBeanRegistry managedBeanRegistry,
			JndiService namingService) {
		this.metadata = metadata;
		this.configurationService = configurationService;
		this.listener = listener;
		this.classLoaderService = classLoaderService;
		this.environmentSynchronizer = environmentSynchronizer;
		this.managedBeanRegistry = managedBeanRegistry;
		this.namingService = namingService;

		/*
		 * Make sure that if a Search integrator is created, it will eventually get closed,
		 * either when the environment is destroyed (see the use of EnvironmentSynchronizer in #sessionFactoryCreated)
		 * or when the session factory is closed (see #sessionFactoryClosed),
		 * whichever happens first.
		 */
		extendedSearchIntegratorFuture.thenAcceptBoth( extendedSearchIntegratorClosingTrigger,
				(integrator, ignored) -> this.cleanup( integrator ) );
	}

	@Override
	public void sessionFactoryCreated(SessionFactory factory) {
		boolean failedBootScheduling = true;
		try {
			listener.initialize( extendedSearchIntegratorFuture );

			if ( environmentSynchronizer != null ) {
				environmentSynchronizer.whenEnvironmentDestroying( () -> {
					// Trigger integrator closing if the integrator actually exists and wasn't already closed
					extendedSearchIntegratorClosingTrigger.complete( null );
				} );
				environmentSynchronizer.whenEnvironmentReady( () -> boot( factory ) );
			}
			else {
				boot( factory );
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
	 * and complete {@link #extendedSearchIntegratorFuture}.
	 * <p>
	 * This method is synchronized in order to avoid booting Hibernate Search
	 * after (or while) the boot has been canceled.
	 *
	 * @param factory The factory on which to graft Hibernate Search.
	 *
	 * @see #cancelBoot()
	 */
	private synchronized void boot(SessionFactory factory) {
		if ( extendedSearchIntegratorFuture.isDone() ) {
			return;
		}
		try {
			HibernateSessionFactoryService sessionService = new DefaultHibernateSessionFactoryService( factory );
			SearchIntegrator searchIntegrator = new SearchIntegratorBuilder()
					.configuration( new SearchConfigurationFromHibernateCore(
							metadata, configurationService, classLoaderService, managedBeanRegistry, sessionService, namingService
							) )
					.buildSearchIntegrator();
			ExtendedSearchIntegrator extendedIntegrator = searchIntegrator.unwrap( ExtendedSearchIntegrator.class );

			this.jmx = new JMXHook( configurationService );
			this.jmx.registerIfEnabled( extendedIntegrator, factory );

			extendedSearchIntegratorFuture.complete( extendedIntegrator );

			//Register the SearchFactory in the ORM ServiceRegistry (for convenience of lookup)
			final SessionFactoryImplementor factoryImplementor = (SessionFactoryImplementor) factory;
			factoryImplementor.getServiceRegistry().getService( SearchFactoryReference.class ).initialize( extendedIntegrator );
		}
		catch (Throwable t) {
			extendedSearchIntegratorFuture.completeExceptionally( t );
			// This will make the SessionFactory abort and close itself
			throw t;
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
	 * @see #boot(SessionFactory)
	 */
	private synchronized void cancelBoot() {
		extendedSearchIntegratorFuture.cancel( false );
	}

	@Override
	public void sessionFactoryClosed(SessionFactory factory) {
		/*
		 * Trigger integrator closing if the integrator actually exists and wasn't already closed
		 * The closing might have been triggered already if an EnvironmentSynchronizer is being used
		 * (see #sessionFactoryCreated).
		 */
		extendedSearchIntegratorClosingTrigger.complete( null );
	}

	private synchronized void cleanup(ExtendedSearchIntegrator extendedIntegrator) {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			if ( extendedIntegrator != null ) {
				closer.push( extendedIntegrator::close );
			}
			if ( jmx != null ) {
				closer.push( jmx::unRegisterIfRegistered );
				jmx = null;
			}
		}
	}

}

