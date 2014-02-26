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
package org.hibernate.search.engine.service.classloading.spi;

import java.io.InputStream;
import java.net.URL;
import java.util.LinkedHashSet;

import org.hibernate.search.engine.service.spi.Service;

/**
 * A service for interacting with class loaders.
 *
 * @author Steve Ebersole
 * @author Hardy Ferentschik
 */
public interface ClassLoaderService extends Service {
	/**
	 * Locate a class by name.
	 *
	 * @param className The name of the class to locate
	 * @param <T> The returned class type.
	 *
	 * @return The class reference
	 *
	 * @throws ClassLoadingException Indicates the class could not be found
	 */
	<T> Class<T> classForName(String className);

	/**
	 * Locate a resource by name (classpath lookup).
	 *
	 * @param name The resource name.
	 *
	 * @return The located URL; may return {@code null} to indicate the resource was not found
	 */
	URL locateResource(String name);

	/**
	 * Locate a resource by name (classpath lookup) and gets its stream.
	 *
	 * @param name The resource name.
	 *
	 * @return The stream of the located resource; may return {@code null} to indicate the resource was not found
	 */
	InputStream locateResourceStream(String name);

	/**
	 * Discovers and instantiates implementations of the named service contract.
	 * <p/>
	 * NOTE : the terms service here is used differently than {@link Service}.  Instead here we are talking about
	 * services as defined by {@link java.util.ServiceLoader}.
	 *
	 * @param serviceContract The java type defining the service contract
	 * @param <T> The type of the service contract
	 *
	 * @return The ordered set of discovered services.
	 */
	<T> LinkedHashSet<T> loadJavaServices(Class<T> serviceContract);
}



