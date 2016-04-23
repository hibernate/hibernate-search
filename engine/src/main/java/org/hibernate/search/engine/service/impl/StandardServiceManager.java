/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.service.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.search.cfg.spi.SearchConfiguration;
import org.hibernate.search.engine.service.classloading.spi.ClassLoaderService;
import org.hibernate.search.engine.service.spi.Service;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.engine.service.spi.Startable;
import org.hibernate.search.engine.service.spi.Stoppable;
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

	private volatile boolean allServicesReleased = false;

	public StandardServiceManager(SearchConfiguration searchConfiguration, BuildContext buildContext) {
		this( searchConfiguration, buildContext, Collections.<Class<? extends Service>, String>emptyMap() );
	}

	public StandardServiceManager(SearchConfiguration searchConfiguration,
			BuildContext buildContext,
			Map<Class<? extends Service>, String> defaultServices) {
		this.buildContext = buildContext;
		this.properties = searchConfiguration.getProperties();
		this.providedServices = createProvidedServices( searchConfiguration );
		this.defaultServices = defaultServices;
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
		if ( providedServices.containsKey( serviceRole ) ) {
			return (S) providedServices.get( serviceRole );
		}

		ServiceWrapper<S> wrapper = (ServiceWrapper<S>) cachedServices.get( serviceRole );
		if ( wrapper == null ) {
			wrapper = createAndCacheWrapper( serviceRole );
		}
		wrapper.startVirtual();
		return wrapper.getService();
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
	public void releaseAllServices() {
		for ( ServiceWrapper wrapper : cachedServices.values() ) {
			wrapper.ensureStopped();
		}
		allServicesReleased = true;
	}

	private Map<Class<? extends Service>, Object> createProvidedServices(SearchConfiguration searchConfiguration) {
		Map<Class<? extends Service>, Object> tmpServices = new HashMap<Class<? extends Service>, Object>();

		for ( Map.Entry<Class<? extends Service>, Object> entry : searchConfiguration.getProvidedServices()
				.entrySet() ) {
			Object service = entry.getValue();
			if ( service instanceof Startable ) {
				throw log.providedServicesCannotImplementStartableOrStoppable(
						service.getClass().getName(),
						Startable.class.getName()
				);
			}
			else if ( service instanceof Stoppable ) {
				throw log.providedServicesCannotImplementStartableOrStoppable(
						service.getClass().getName(),
						Stoppable.class.getName()
				);
			}
			else {
				tmpServices.put( entry.getKey(), entry.getValue() );
			}
		}

		if ( tmpServices.containsKey( ClassLoaderService.class ) ) {
			throw log.classLoaderServiceContainedInProvidedServicesException();
		}
		else {
			tmpServices.put( ClassLoaderService.class, searchConfiguration.getClassLoaderService() );
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
				if ( status != ServiceStatus.RUNNING ) {
					stateExpectedFailure();
				}
				stopAndRemoveFromCache();
			}
			else if ( status != ServiceStatus.RUNNING ) {
				//Could happen on circular dependencies
				stateExpectedFailure();
			}
		}

		synchronized void ensureStopped() {
			if ( status != ServiceStatus.STOPPED ) {
				log.serviceProviderNotReleased( serviceClass );
				stopAndRemoveFromCache();
			}
		}

		private void stopAndRemoveFromCache() {
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
				cachedServices.remove( serviceClass );
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
}
