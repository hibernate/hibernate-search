/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.environment.classpath.spi;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;

/**
 * A classloader which keeps an ordered list of aggregated classloaders.
 * <p>
 * This is especially useful in modular environments such as WildFly
 * where some classes may not be accessible from Hibernate Search's classloader,
 * for example custom user components such as bridges.
 *
 * @author Steve Ebersole
 * @author Hardy Ferentschik
 * @author Sanne Grinovero
 */
public final class AggregatedClassLoader extends ClassLoader {
	private ClassLoader[] individualClassLoaders;

	public static AggregatedClassLoader createDefault() {
		final LinkedHashSet<ClassLoader> orderedClassLoaderSet = new LinkedHashSet<>();

		//  adding known class-loaders...
		orderedClassLoaderSet.add( AggregatedClassLoader.class.getClassLoader() );

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
		return new AggregatedClassLoader(
				orderedClassLoaderSet.toArray( new ClassLoader[0] )
		);
	}

	private AggregatedClassLoader(ClassLoader... classLoaders) {
		super( null );
		individualClassLoaders = classLoaders;
	}

	@Override
	public Enumeration<URL> getResources(String name) throws IOException {
		final HashSet<URL> resourceUrls = new HashSet<>();

		for ( ClassLoader classLoader : individualClassLoaders ) {
			final Enumeration<URL> urls = classLoader.getResources( name );
			while ( urls.hasMoreElements() ) {
				resourceUrls.add( urls.nextElement() );
			}
		}

		return new Enumeration<URL>() {
			final Iterator<URL> resourceUrlIterator = resourceUrls.iterator();

			@Override
			public boolean hasMoreElements() {
				return resourceUrlIterator.hasNext();
			}

			@Override
			public URL nextElement() {
				return resourceUrlIterator.next();
			}
		};
	}

	@Override
	protected URL findResource(String name) {
		for ( ClassLoader classLoader : individualClassLoaders ) {
			final URL resource = classLoader.getResource( name );
			if ( resource != null ) {
				return resource;
			}
		}
		return super.findResource( name );
	}

	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		for ( ClassLoader classLoader : individualClassLoaders ) {
			try {
				return classLoader.loadClass( name );
			}
			catch (Exception | LinkageError ignore) {
				// Ignore
			}
		}
		throw new ClassNotFoundException( "Could not load requested class : " + name );
	}

	void addAllTo(Collection<ClassLoader> classLoaders) {
		Collections.addAll( classLoaders, individualClassLoaders );
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
