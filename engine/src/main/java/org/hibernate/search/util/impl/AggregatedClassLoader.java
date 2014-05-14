/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;

/**
 * A classloader which keeps a ordered list of aggregated classloaders.
 *
 * @author Steve Ebersole
 * @author Hardy Ferentschik
 */
public class AggregatedClassLoader extends ClassLoader {
	private ClassLoader[] individualClassLoaders;

	public AggregatedClassLoader(ClassLoader... classLoaders) {
		super( null );
		individualClassLoaders = classLoaders;
	}

	@Override
	public Enumeration<URL> getResources(String name) throws IOException {
		final HashSet<URL> resourceUrls = new HashSet<URL>();

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
			catch (Exception ignore) {
			}
		}
		throw new ClassNotFoundException( "Could not load requested class : " + name );
	}

	public void destroy() {
		individualClassLoaders = null;
	}
}
