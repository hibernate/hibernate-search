/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.search.cfg.spi.SearchConfiguration;
import org.hibernate.search.engine.service.beanresolver.impl.ReflectionBeanResolver;
import org.hibernate.search.engine.service.beanresolver.impl.ReflectionFallbackBeanResolver;
import org.hibernate.search.engine.service.beanresolver.spi.BeanResolver;
import org.hibernate.search.engine.service.classloading.spi.ClassLoaderService;
import org.hibernate.search.engine.service.spi.Service;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.engine.service.spi.ServiceReference;
import org.hibernate.search.engine.service.spi.Startable;
import org.hibernate.search.engine.service.spi.Stoppable;
import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.spi.BuildContext;
import org.hibernate.search.util.StringHelper;
import org.hibernate.search.util.impl.ClassLoaderHelper;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Default implementation of the {@code ServiceManager} interface.
 *
 * @author Emmanuel Bernard
 * @author Sanne Grinovero
 * @author Hardy Ferentschik
 */
public class StandardServiceManager implements ServiceManager {
	private static final Log log = LoggerFactory.make();

	private final Properties properties;
	private final BuildContext buildContext;
	private final ConcurrentHashMap<Class<?>, ServiceWrapper<?>> cachedServices = new ConcurrentHashMap<Class<?>, ServiceWrapper<?>>();
	private final Map<Class<? extends Service>, Object> providedServices;
	private final Map<Class<? extends Service>, String> defaultServices;
	private final ClassLoaderService classloaderService;
	private final BeanResolver beanResolver;
	private final boolean failOnUnreleasedService;

	private volatile boolean allServicesReleased = false;

	public StandardServiceManager(SearchConfiguration searchConfiguration, BuildContext buildContext) {
		this( searchConfiguration, buildContext, Collections.<Class<? extends Service>, String>emptyMap() );
	}

	public StandardServiceManager(SearchConfiguration searchConfiguration,
			BuildContext buildContext,
			Map<Class<? extends Service>, String> defaultServices) {
		this.buildContext = buildContext;
		this.properties = searchConfiguration.getProperties();
		this.defaultServices = defaultServices;
		this.classloaderService = searchConfiguration.getClassLoaderService();
		BeanResolver configuredBeanResolver = searchConfiguration.getBeanResolver();
		ReflectionBeanResolver reflectionBeanResolver = new ReflectionBeanResolver();
		if ( configuredBeanResolver != null ) {
			this.beanResolver = new ReflectionFallbackBeanResolver( configuredBeanResolver, reflectionBeanResolver );
		}
		else {
			this.beanResolver = reflectionBeanResolver;
		}
		this.providedServices = createProvidedServices( searchConfiguration ); // Requires beanResolver and classloaderService to be set
		this.failOnUnreleasedService = Boolean.getBoolean( "org.hibernate.search.fail_on_unreleased_service" );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <S extends Service> S requestService(Class<S> serviceRole) {
		if ( serviceRole == null ) {
			throw new IllegalArgumentException( "'null' is not a valid service role" );
		}

		if ( allServicesReleased ) {
			throw log.serviceRequestedAfterReleasedAllWasCalled();
		}

		// provided services have priority over managed services
		final Object providedService = providedServices.get( serviceRole );
		if ( providedService != null ) {
			return (S) providedService;
		}

		ServiceWrapper<S> wrapper = (ServiceWrapper<S>) cachedServices.get( serviceRole );
		if ( wrapper == null ) {
			wrapper = createAndCacheWrapper( serviceRole );
		}
		wrapper.startVirtual();
		return wrapper.getService();
	}

	@Override
	public <S extends Service> ServiceReference<S> requestReference(Class<S> serviceRole) {
		return new ServiceReference<>( this, serviceRole );
	}

	@Override
	public <S extends Service> void releaseService(Class<S> serviceRole) {
		if ( serviceRole == null ) {
			throw new IllegalArgumentException( "'null' is not a valid service role" );
		}

		if ( providedServices.containsKey( serviceRole ) ) {
			return;
		}

		ServiceWrapper wrapper = cachedServices.get( serviceRole );
		if ( wrapper != null ) {
			wrapper.stopVirtual();
		}
	}

	@Override
	public synchronized void releaseAllServices() {
		for ( ServiceWrapper<?> wrapper : cachedServices.values() ) {
			/*
			 * Perform an additional stopVirtual, to remove the extra usage token granted at first initialization,
			 * which keeps the service to be really stopped when it's released by the service client, yet we're not shutting down
			 * the Search engine yet.
			 */
			wrapper.stopVirtual();
		}

		/*
		 * If everything went well, the previous pass should have brought every
		 * user count to 0 for every service, so every service should have been stopped.
		 * This should also have chained-released services used by services, which
		 * ultimately should have stopped everything.
		 */

		/*
		 * Second pass to check for still-running services and forcefully stop them.
		 */
		List<String> unreleasedServicesToReport = failOnUnreleasedService ? new ArrayList<String>() : null;
		for ( ServiceWrapper<?> wrapper : cachedServices.values() ) {
			synchronized ( wrapper ) {
				if ( wrapper.status != ServiceStatus.STOPPED ) {
					log.serviceProviderNotReleased( wrapper.serviceClass );
					wrapper.stopReal();
					if ( unreleasedServicesToReport != null ) {
						unreleasedServicesToReport.add( wrapper.serviceClass.getName() );
					}
				}
			}
		}

		cachedServices.clear();
		allServicesReleased = true;

		if ( failOnUnreleasedService && !unreleasedServicesToReport.isEmpty() ) {
			throw new AssertionFailure( "The following services have been used but not released: "
					+ unreleasedServicesToReport );
		}
	}

	private Map<Class<? extends Service>, Object> createProvidedServices(SearchConfiguration searchConfiguration) {
		Map<Class<? extends Service>, Object> tmpServices = new HashMap<Class<? extends Service>, Object>(
				searchConfiguration.getProvidedServices()
				);

		if ( tmpServices.containsKey( ClassLoaderService.class ) ) {
			throw log.classLoaderServiceContainedInProvidedServicesException();
		}
		else {
			tmpServices.put( ClassLoaderService.class, this.classloaderService );
		}

		if ( tmpServices.containsKey( BeanResolver.class ) ) {
			throw log.beanResolverContainedInProvidedServicesException();
		}
		else {
			tmpServices.put( BeanResolver.class, this.beanResolver );
		}

		return Collections.unmodifiableMap( tmpServices );
	}

	/**
	 * The 'synchronized' is necessary to avoid loading the same service in parallel: enumerating service
	 * implementations is not threadsafe when delegating to Hibernate ORM's org.hibernate.boot.registry.classloading.spi.ClassLoaderService
	 */
	private synchronized <S extends Service> ServiceWrapper<S> createAndCacheWrapper(Class<S> serviceRole) {
		//Check again, for concurrent usage:
		ServiceWrapper<S> existingWrapper = (ServiceWrapper<S>) cachedServices.get( serviceRole );
		if ( existingWrapper != null ) {
			return existingWrapper;
		}
		Set<S> services = new HashSet<>();
		for ( S service : requestService( ClassLoaderService.class ).loadJavaServices( serviceRole ) ) {
			services.add( service );
		}

		if ( services.size() == 0 ) {
			tryLoadingDefaultService( serviceRole, services );
		}
		else if ( services.size() > 1 ) {
			throw log.getMultipleServiceImplementationsException(
					serviceRole.toString(),
					StringHelper.join( services, "," )
			);
		}
		S service = services.iterator().next();
		ServiceWrapper<S> wrapper = new ServiceWrapper<S>( service, serviceRole, buildContext );
		@SuppressWarnings("unchecked")
		ServiceWrapper<S> previousWrapper = (ServiceWrapper<S>) cachedServices.putIfAbsent( serviceRole, wrapper );
		if ( previousWrapper != null ) {
			wrapper = previousWrapper;
		}
		else {
			//Initialize the service usage counter with an additional usage token, on top of the one granted by the service request:
			wrapper.startVirtual();
		}
		return wrapper;
	}

	private <S extends Service> void tryLoadingDefaultService(Class<S> serviceRole, Set<S> services) {
		// there is no loadable service. Check whether we have a default one we can instantiate
		if ( defaultServices.containsKey( serviceRole ) ) {
			S service = ClassLoaderHelper.instanceFromName(
					serviceRole,
					defaultServices.get( serviceRole ),
					"default service",
					this
			);
			services.add( service );
		}
		else {
			throw log.getNoServiceImplementationFoundException( serviceRole.toString() );
		}
	}

	private class ServiceWrapper<S> {
		private final S service;
		private final BuildContext context;
		private final Class<S> serviceClass;

		private int userCounter = 0;
		private ServiceStatus status = ServiceStatus.STOPPED;

		ServiceWrapper(S service, Class<S> serviceClass, BuildContext context) {
			this.service = service;
			this.context = context;
			this.serviceClass = serviceClass;
		}

		synchronized S getService() {
			if ( status != ServiceStatus.RUNNING ) {
				stateExpectedFailure();
			}
			return service;
		}

		/**
		 * Virtual call to the start method: only actually starts the
		 * service when bumping up the counter of start requests from
		 * zero. Subsequent start requests will simply increment the
		 * counter, so that we can eventually tear down the services
		 * in reverse order to make dependency graphs happy.
		 *
		 * Make sure to invoke startVirtual() both on service request,
		 * and on creation of the wrapper so that the first request
		 * accounts for two usage tokens rather than one.
		 * The shutdown process will similarly invoke stopVirtual() an
		 * additional time; this is to prevent services from starting
		 * and stopping frequently at runtime.
		 */
		synchronized void startVirtual() {
			int previousValue = userCounter;
			userCounter++;
			if ( previousValue == 0 ) {
				if ( status != ServiceStatus.STOPPED ) {
					stateExpectedFailure();
				}
				startService( service );
			}
			if ( status != ServiceStatus.RUNNING ) {
				//Could happen on circular dependencies
				stateExpectedFailure();
			}
		}

		synchronized void stopVirtual() {
			userCounter--;
			if ( userCounter == 0 ) {
				//Do not check for the expected status in this case: we don't want a previous service start failure to
				//prevent us to further attempt to stop services.
				stopReal();
			}
		}

		synchronized void stopReal() {
			status = ServiceStatus.STOPPING;
			try {
				if ( service instanceof Stoppable ) {
					( (Stoppable) service ).stop();
				}
			}
			catch (Exception e) {
				log.stopServiceFailed( serviceClass, e );
			}
			finally {
				status = ServiceStatus.STOPPED;
			}
		}

		private void startService(final S service) {
			status = ServiceStatus.STARTING;
			if ( service instanceof Startable ) {
				( (Startable) service ).start( properties, context );
			}
			status = ServiceStatus.RUNNING;
		}

		private void stateExpectedFailure() {
			throw log.getUnexpectedServiceStatusException( status.name(), service.toString() );
		}
	}

	private enum ServiceStatus {
		RUNNING, STOPPED, STARTING, STOPPING
	}

	@Override
	public ClassLoaderService getClassLoaderService() {
		return classloaderService;
	}

	@Override
	public BeanResolver getBeanResolver() {
		return beanResolver;
	}

}
