/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
package org.hibernate.search.test.util;

import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.ServiceLoader;

import org.hibernate.search.engine.service.classloading.spi.ClassLoaderService;
import org.hibernate.search.engine.service.classloading.spi.ClassLoadingException;
import org.hibernate.search.engine.service.spi.Stoppable;
import org.hibernate.search.util.impl.AggregatedClassLoader;

/**
 * Implementation of {@code ClassLoaderService} for testing.
 *
 * @author Steve Ebersole
 * @author Hardy Ferentschik
 */
public class TestClassLoaderService implements ClassLoaderService, Stoppable {

	private final Map<Class, ServiceLoader> serviceLoaders = new HashMap<Class, ServiceLoader>();
	private AggregatedClassLoader aggregatedClassLoader;

	/**
	 * Constructs a ClassLoaderServiceImpl with standard set-up
	 */
	public TestClassLoaderService() {
		this( TestClassLoaderService.class.getClassLoader() );
	}

	/**
	 * Constructs a ClassLoaderServiceImpl with the given ClassLoader
	 *
	 * @param classLoader The ClassLoader to use
	 */
	public TestClassLoaderService(ClassLoader classLoader) {
		this( Collections.singletonList( classLoader ) );
	}

	/**
	 * Constructs a ClassLoaderServiceImpl with the given ClassLoader instances
	 *
	 * @param providedClassLoaders The ClassLoader instances to use
	 */
	public TestClassLoaderService(Collection<ClassLoader> providedClassLoaders) {
		final LinkedHashSet<ClassLoader> orderedClassLoaderSet = new LinkedHashSet<ClassLoader>();

		// first, add all provided class loaders, if any
		if ( providedClassLoaders != null ) {
			for ( ClassLoader classLoader : providedClassLoaders ) {
				if ( classLoader != null ) {
					orderedClassLoaderSet.add( classLoader );
				}
			}
		}

		// normalize adding known class-loaders...
		orderedClassLoaderSet.add( TestClassLoaderService.class.getClassLoader() );

		// then the TCCL, if one...
		final ClassLoader tccl = locateTCCL();
		if ( tccl != null ) {
			orderedClassLoaderSet.add( tccl );
		}

		// finally the system classloader
		final ClassLoader sysClassLoader = locateSystemClassLoader();
		if ( sysClassLoader != null ) {
			orderedClassLoaderSet.add( sysClassLoader );
		}

		// now build the aggregated class loader...
		this.aggregatedClassLoader = new AggregatedClassLoader(
				orderedClassLoaderSet.toArray(
						new ClassLoader[orderedClassLoaderSet.size()]
				)
		);
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public <T> Class<T> classForName(String className) {
		try {
			return (Class<T>) Class.forName( className, true, aggregatedClassLoader );
		}
		catch (Exception e) {
			throw new ClassLoadingException( "Unable to load class [" + className + "]", e );
		}
	}

	@Override
	public URL locateResource(String name) {
		// first we try name as a URL
		try {
			return new URL( name );
		}
		catch (Exception ignore) {
		}

		try {
			return aggregatedClassLoader.getResource( name );
		}
		catch (Exception ignore) {
		}

		return null;
	}

	@Override
	public InputStream locateResourceStream(String name) {
		// first we try name as a URL
		try {
			return new URL( name ).openStream();
		}
		catch (Exception ignore) {
		}

		try {
			final InputStream stream = aggregatedClassLoader.getResourceAsStream( name );
			if ( stream != null ) {
				return stream;
			}
		}
		catch (Exception ignore) {
		}

		final String stripped = name.startsWith( "/" ) ? name.substring( 1 ) : null;

		if ( stripped != null ) {
			try {
				return new URL( stripped ).openStream();
			}
			catch (Exception ignore) {
			}

			try {
				final InputStream stream = aggregatedClassLoader.getResourceAsStream( stripped );
				if ( stream != null ) {
					return stream;
				}
			}
			catch (Exception ignore) {
			}
		}

		return null;
	}


	@Override
	@SuppressWarnings("unchecked")
	public <S> LinkedHashSet<S> loadJavaServices(Class<S> serviceContract) {
		ServiceLoader<S> serviceLoader;
		if ( serviceLoaders.containsKey( serviceContract ) ) {
			serviceLoader = serviceLoaders.get( serviceContract );
		}
		else {
			serviceLoader = ServiceLoader.load( serviceContract, aggregatedClassLoader );
			serviceLoaders.put( serviceContract, serviceLoader );
		}

		final LinkedHashSet<S> services = new LinkedHashSet<S>();
		for ( S service : serviceLoader ) {
			services.add( service );
		}
		return services;
	}

	@Override
	public void stop() {
		for ( ServiceLoader serviceLoader : serviceLoaders.values() ) {
			serviceLoader.reload(); // clear service loader providers
		}
		serviceLoaders.clear();

		if ( aggregatedClassLoader != null ) {
			aggregatedClassLoader.destroy();
			aggregatedClassLoader = null;
		}
	}

	private static ClassLoader locateSystemClassLoader() {
		try {
			return ClassLoader.getSystemClassLoader();
		}
		catch (Exception e) {
			return null;
		}
	}

	private static ClassLoader locateTCCL() {
		try {
			return Thread.currentThread().getContextClassLoader();
		}
		catch (Exception e) {
			return null;
		}
	}
}



