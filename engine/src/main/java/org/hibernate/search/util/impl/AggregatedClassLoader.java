/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
