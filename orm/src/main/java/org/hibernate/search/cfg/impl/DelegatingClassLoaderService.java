/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.cfg.impl;

import java.io.InputStream;
import java.net.URL;
import java.util.LinkedHashSet;

import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.search.engine.service.classloading.impl.DefaultClassLoaderService;
import org.hibernate.search.engine.service.classloading.spi.ClassLoaderService;

/**
 * An implementation of class loader service which delegates to the ORM provided {@code ClassLoaderService}. If class,
 * resource or service loading fails in ORM class loader, the current class loader is checked.
 *
 * @author Hardy Ferentschik
 */
public class DelegatingClassLoaderService implements ClassLoaderService {
	/**
	 * {@code ClassLoaderService] as provided by Hibernate ORM. This is the class loader which we attempt to use first.
	 */
	private final org.hibernate.boot.registry.classloading.spi.ClassLoaderService hibernateClassLoaderService;

	/**
	 * A Search internal class loader service which in particular tries to use the current class loader. This can be
	 * necessary in case the ORM class loader can due to modularity not access the required resources
	 */
	private final ClassLoaderService internalClassLoaderService;


	public DelegatingClassLoaderService(org.hibernate.boot.registry.classloading.spi.ClassLoaderService hibernateClassLoaderService) {
		this.hibernateClassLoaderService = hibernateClassLoaderService;
		this.internalClassLoaderService = new DefaultClassLoaderService();
	}

	@Override
	public <T> Class<T> classForName(String className) {
		try {
			return hibernateClassLoaderService.classForName( className );
		}
		catch (ClassLoadingException e) {
			return internalClassLoaderService.classForName( className );
		}
	}

	@Override
	public URL locateResource(String name) {
		URL url = hibernateClassLoaderService.locateResource( name );
		if ( url == null ) {
			url = internalClassLoaderService.locateResource( name );
		}
		return url;
	}

	@Override
	public InputStream locateResourceStream(String name) {
		InputStream in = hibernateClassLoaderService.locateResourceStream( name );
		if ( in == null ) {
			in = internalClassLoaderService.locateResourceStream( name );
		}
		return in;
	}

	@Override
	public <T> LinkedHashSet<T> loadJavaServices(Class<T> serviceContract) {
		// when it comes to services, we need to search in both services and the de-duplicate
		// however, we cannot rely on 'equals' for comparison. Instead compare class names
		LinkedHashSet<T> servicesFromORMCLassLoader = new LinkedHashSet<T>(
				hibernateClassLoaderService.loadJavaServices(
						serviceContract
				)
		);
		LinkedHashSet<T> services = new LinkedHashSet<T>( internalClassLoaderService.loadJavaServices( serviceContract ) );

		for ( T serviceInstance : servicesFromORMCLassLoader ) {
			if ( !contains( services, serviceInstance ) ) {
				services.add( serviceInstance );
			}
		}

		return services;
	}

	private <T> boolean contains(LinkedHashSet<T> services, T serviceInstance) {
		for ( T service : services ) {
			if ( service.getClass().getName().equals( serviceInstance.getClass().getName() ) ) {
				return true;
			}
		}
		return false;
	}
}


