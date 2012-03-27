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
package org.hibernate.search.engine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import org.hibernate.search.cfg.spi.SearchConfiguration;
import org.hibernate.search.util.impl.ClassLoaderHelper;
import org.hibernate.search.util.logging.impl.Log;

import org.hibernate.search.SearchException;
import org.hibernate.search.spi.ServiceProvider;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * @author Emmanuel Bernard
 */
public class ServiceManager {
	private static final String SERVICES_FILE = "META-INF/services/" + ServiceProvider.class.getName();
	private static final Log log = LoggerFactory.make();

	//barrier protected by the Hibernate Search instantiation
	private final Map<Class<ServiceProvider<?>>,ServiceProviderWrapper> managedProviders = new HashMap<Class<ServiceProvider<?>>,ServiceProviderWrapper>();
	private final Map<Class<? extends ServiceProvider<?>>,Object> providedProviders = new HashMap<Class<? extends ServiceProvider<?>>,Object>();
	private final Properties properties;

	public ServiceManager(SearchConfiguration cfg) {
		this.properties = cfg.getProperties();
		this.providedProviders.putAll( cfg.getProvidedServices() );
		listAndInstantiateServiceProviders();
	}

	private void listAndInstantiateServiceProviders() {
		//get list of services available
		final Enumeration<URL> resources = ClassLoaderHelper.getResources( SERVICES_FILE, ServiceManager.class );
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
							final ServiceProvider<?> serviceProvider = ClassLoaderHelper.instanceFromName(
									ServiceProvider.class, name, ServiceManager.class, "service provider"
							);
							@SuppressWarnings( "unchecked")
							final Class<ServiceProvider<?>> serviceProviderClass =
									( Class<ServiceProvider<?>> ) serviceProvider.getClass();
							managedProviders.put( serviceProviderClass, new ServiceProviderWrapper(serviceProvider) );
						}
						name = reader.readLine();
					}
				}
				finally {
					stream.close();
				}
			}
		}
		catch ( IOException e ) {
			throw new SearchException( "Unable to read " + SERVICES_FILE, e );
		}
	}

	public <T> T requestService(Class<? extends ServiceProvider<T>> serviceProviderClass) {
		//provided services have priority over managed services
		if ( providedProviders.containsKey( serviceProviderClass ) ) {
			//we use containsKey as the service itself might be null
			//TODO be safer and throw a cleaner exception
			return (T) providedProviders.get( serviceProviderClass );
		}

		@SuppressWarnings( "unchecked")
		final ServiceProviderWrapper wrapper = managedProviders.get( serviceProviderClass );
		if (wrapper == null) {
			throw new SearchException( "Unable to find service related to " + serviceProviderClass);
		}
		wrapper.increaseCounter();
		return (T) wrapper.getServiceProvider().getService();
	}

	public void releaseService(Class<? extends ServiceProvider<?>> serviceProviderClass) {
		//provided services have priority over managed services
		if ( providedProviders.containsKey( serviceProviderClass ) ) {
			return;
		}
		
		final ServiceProviderWrapper wrapper = managedProviders.get( serviceProviderClass );
		if ( wrapper == null ) {
			throw new SearchException( "Unable to find service related to " + serviceProviderClass);
		}
		wrapper.decreaseCounter();
	}

	public void stopServices() {
		for ( ServiceProviderWrapper wrapper :  managedProviders.values() ) {
			if ( wrapper.getCounter() != 0 ) {
				log.serviceProviderNotReleased( wrapper.getServiceProvider().getClass() );
			}
			try {
				wrapper.getServiceProvider().stop();
			}
			catch ( Exception e ) {
				log.stopServiceFailed( wrapper.getServiceProvider().getClass(), e );
			}
		}
	}

	private class ServiceProviderWrapper {
		private final ServiceProvider<?> serviceProvider;
		private final AtomicInteger counter = new AtomicInteger( 0 );


		public ServiceProviderWrapper(ServiceProvider<?> serviceProvider) {
			this.serviceProvider = serviceProvider;
		}

		public ServiceProvider<?> getServiceProvider() {
			return serviceProvider;
		}

		synchronized void increaseCounter() {
			final int oldValue = counter.getAndIncrement();
			if ( oldValue == 0 ) {
				serviceProvider.start( properties );
			}
		}

		int getCounter() {
			return counter.get();
		}

		void decreaseCounter() {
			counter.getAndDecrement();
		}
	}
}
