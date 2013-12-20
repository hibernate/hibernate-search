/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search.engine.service.impl;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.search.cfg.spi.SearchConfiguration;
import org.hibernate.search.engine.service.spi.Service;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.engine.service.spi.Startable;
import org.hibernate.search.engine.service.spi.Stoppable;
import org.hibernate.search.spi.BuildContext;
import org.hibernate.search.util.StringHelper;
import org.hibernate.search.util.impl.AggregatedClassLoader;
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
	private AggregatedClassLoader aggregatedClassLoader;

	private final ConcurrentHashMap<Class<?>, ServiceWrapper<?>> cachedServices = new ConcurrentHashMap<Class<?>, ServiceWrapper<?>>();
	private final Map<Class<? extends Service>, Object> providedServices;

	public StandardServiceManager(SearchConfiguration cfg, BuildContext buildContext) {
		this.properties = cfg.getProperties();
		this.providedServices = Collections.unmodifiableMap( cfg.getProvidedServices() );
		this.buildContext = buildContext;

		this.aggregatedClassLoader = new AggregatedClassLoader(
				Thread.currentThread().getContextClassLoader(),
				this.getClass().getClassLoader()
		);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <S extends Service> S requestService(Class<S> serviceRole) {
		if ( serviceRole == null ) {
			throw new IllegalArgumentException( "'null' is not a valid service role" );
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

		//provided services have priority over managed services
		if ( providedServices.containsKey( providedServices ) ) {
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
	}

	@SuppressWarnings("unchecked")
	private <S extends Service> Set<S> loadJavaServices(Class<S> serviceContract) {
		ServiceLoader<S> serviceLoader = ServiceLoader.load( serviceContract, aggregatedClassLoader );
		final HashSet<S> services = new LinkedHashSet<S>();
		for ( S service : serviceLoader ) {
			services.add( service );
		}
		return services;
	}

	private <S extends Service> ServiceWrapper<S> createAndCacheWrapper(Class<S> serviceRole) {
		ServiceWrapper<S> wrapper;
		Set<S> services = loadJavaServices( serviceRole );

		if ( services.size() == 0 ) {
			throw log.getNoServiceImplementationFoundException( serviceRole.toString() );
		}
		else if ( services.size() > 1 ) {
			throw log.getMultipleServiceImplementationsException(
					serviceRole.toString(),
					StringHelper.join( services, "," )
			);
		}
		wrapper = new ServiceWrapper<S>( services.iterator().next(), serviceRole, buildContext );
		@SuppressWarnings("unchecked")
		ServiceWrapper<S> previousWrapper = (ServiceWrapper<S>) cachedServices.putIfAbsent( serviceRole, wrapper );
		if ( previousWrapper != null ) {
			wrapper = previousWrapper;
		}
		return wrapper;
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
