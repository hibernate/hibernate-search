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
package org.hibernate.search.engine.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.SearchException;
import org.hibernate.search.cfg.spi.SearchConfiguration;
import org.hibernate.search.engine.ServiceManager;
import org.hibernate.search.spi.BuildContext;
import org.hibernate.search.spi.ServiceProvider;
import org.hibernate.search.util.impl.ClassLoaderHelper;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * @author Emmanuel Bernard
 * @author Sanne Grinovero
 */
public class StandardServiceManager implements ServiceManager {
	private static final String SERVICES_FILE = "META-INF/services/" + ServiceProvider.class.getName();
	private static final Log log = LoggerFactory.make();

	//barrier protected by the Hibernate Search instantiation
	private final HashSet<Class<?>> availableProviders = new HashSet<Class<?>>();
	private final ConcurrentHashMap<Class<?>, ServiceProviderWrapper<?>> managedProviders = new ConcurrentHashMap<Class<?>, ServiceProviderWrapper<?>>();
	private final Map<Class<? extends ServiceProvider<?>>, Object> providedProviders = new HashMap<Class<? extends ServiceProvider<?>>, Object>();
	private final Properties properties;

	public StandardServiceManager(SearchConfiguration cfg) {
		this.properties = cfg.getProperties();
		this.providedProviders.putAll( cfg.getProvidedServices() );
		listAndInstantiateServiceProviders();
	}

	private void listAndInstantiateServiceProviders() {
		//get list of services available
		final Enumeration<URL> resources = ClassLoaderHelper.getResources( SERVICES_FILE, StandardServiceManager.class );
		String name;
		try {
			while ( resources.hasMoreElements() ) {
				URL url = resources.nextElement();
				InputStream stream = url.openStream();
				try {
					BufferedReader reader = new BufferedReader( new InputStreamReader( stream ), 100 );
					name = reader.readLine();
					while ( name != null ) {
						name = name.trim();
						if ( !name.startsWith( "#" ) ) {
							final Class<?> serviceProviderClass =
									ClassLoaderHelper.classForName( name, StandardServiceManager.class.getClassLoader(), "service provider" );
							availableProviders.add( serviceProviderClass );
						}
						name = reader.readLine();
					}
				}
				finally {
					stream.close();
				}
			}
		}
		catch (IOException e) {
			throw new SearchException( "Unable to read " + SERVICES_FILE, e );
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T requestService(Class<? extends ServiceProvider<T>> serviceProviderClass, BuildContext context) {
		//provided services have priority over managed services
		if ( providedProviders.containsKey( serviceProviderClass ) ) {
			//we use containsKey as the service itself might be null
			//TODO be safer and throw a cleaner exception
			return (T) providedProviders.get( serviceProviderClass );
		}

		ServiceProviderWrapper<T> wrapper = (ServiceProviderWrapper<T>) managedProviders.get( serviceProviderClass );
		if ( wrapper == null ) {
			if ( availableProviders.contains( serviceProviderClass ) ) {
				ServiceProvider<T> serviceProvider = ClassLoaderHelper.instanceFromClass(
						ServiceProvider.class,
						serviceProviderClass,
						"service provider"
				);
				wrapper = new ServiceProviderWrapper<T>( serviceProvider, context, serviceProviderClass );
				managedProviders.putIfAbsent( serviceProviderClass, wrapper );
			}
			else {
				throw new SearchException( "Unable to find service related to " + serviceProviderClass );
			}
		}
		wrapper = (ServiceProviderWrapper<T>) managedProviders.get( serviceProviderClass );
		wrapper.startVirtual();
		return wrapper.getService();
	}

	@Override
	public void releaseService(Class<? extends ServiceProvider<?>> serviceProviderClass) {
		//provided services have priority over managed services
		if ( providedProviders.containsKey( serviceProviderClass ) ) {
			return;
		}

		final ServiceProviderWrapper wrapper = managedProviders.get( serviceProviderClass );
		if ( wrapper == null ) {
			throw new AssertionFailure( "Unable to find service related to " + serviceProviderClass);
		}

		wrapper.stopVirtual();
	}

	@Override
	public void stopServices() {
		for ( ServiceProviderWrapper wrapper : managedProviders.values() ) {
			wrapper.ensureStopped();
		}
	}

	private class ServiceProviderWrapper<S> {

		private final ServiceProvider<S> serviceProvider;
		private final BuildContext context;
		private final Class<? extends ServiceProvider<S>> serviceProviderClass;

		private int userCounter = 0;
		private ServiceStatus status = ServiceStatus.STOPPED;

		ServiceProviderWrapper(ServiceProvider<S> serviceProvider, BuildContext context, Class<? extends ServiceProvider<S>> serviceProviderClass) {
			this.serviceProvider = serviceProvider;
			this.context = context;
			this.serviceProviderClass = serviceProviderClass;
		}

		synchronized S getService() {
			if ( status != ServiceStatus.RUNNING ) {
				stateExpectedFailure();
			}
			return serviceProvider.getService();
		}

		synchronized void startVirtual() {
			int previousValue = userCounter;
			userCounter++;
			if ( previousValue == 0 ) {
				if ( status != ServiceStatus.STOPPED ) {
					stateExpectedFailure();
				}
				status = ServiceStatus.STARTING;
				serviceProvider.start( properties, context );
				status = ServiceStatus.RUNNING;
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
				status = ServiceStatus.STOPPING;
				forceStop();
				status = ServiceStatus.STOPPED;
				managedProviders.remove( serviceProviderClass );
			}
			else if ( status != ServiceStatus.RUNNING ) {
				//Could happen on circular dependencies
				stateExpectedFailure();
			}
		}

		synchronized void ensureStopped() {
			if ( status != ServiceStatus.STOPPED ) {
				log.serviceProviderNotReleased( serviceProviderClass );
				forceStop();
			}
		}

		private void forceStop() {
			try {
				serviceProvider.stop();
			}
			catch (Exception e) {
				log.stopServiceFailed( serviceProviderClass, e );
			}
		}

		private void stateExpectedFailure() {
			throw new AssertionFailure( "Unexpected status '" + status + "' for serviceProvider '" + serviceProvider + "'." +
					" Check for circular dependencies or unreleased resources in your services." );
		}
	}

	private enum ServiceStatus {
		RUNNING, STOPPED, STARTING, STOPPING
	}
}
