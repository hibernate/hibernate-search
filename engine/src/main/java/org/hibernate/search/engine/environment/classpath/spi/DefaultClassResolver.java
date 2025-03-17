/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.environment.classpath.spi;

import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.Set;

import org.hibernate.search.engine.logging.impl.EngineMiscLog;


/**
 * Default implementation of {@code ClassResolver} relying on an {@link AggregatedClassLoader}.
 *
 * @author Hardy Ferentschik
 */
public final class DefaultClassResolver implements ClassResolver {

	private final AggregatedClassLoader aggregatedClassLoader;

	public static ClassResolver create(AggregatedClassLoader aggregatedClassLoader) {
		return new DefaultClassResolver( aggregatedClassLoader );
	}

	private DefaultClassResolver(AggregatedClassLoader aggregatedClassLoader) {
		this.aggregatedClassLoader = aggregatedClassLoader;
	}

	@Override
	public Class<?> classForName(String className) {
		try {
			return Class.forName( className, true, aggregatedClassLoader );
		}
		catch (Exception | LinkageError e) {
			throw EngineMiscLog.INSTANCE.unableToLoadTheClass( className, e.getMessage(), e );
		}
	}

	@Override
	public Package packageForName(String packageName) {
		try {
			return Class.forName( packageName + ".package-info", true, aggregatedClassLoader )
					.getPackage();
		}
		catch (Exception | LinkageError e) {
			return null;
		}
	}

	@Override
	public URL locateResource(String resourceName) {
		try {
			return aggregatedClassLoader.getResource( resourceName );
		}
		catch (Exception e) {
			return null;
		}
	}

	@Override
	public <S> Collection<S> loadJavaServices(Class<S> serviceType) {
		ServiceLoader<S> loadedServices = ServiceLoader.load( serviceType, aggregatedClassLoader );
		Iterator<S> iterator = loadedServices.iterator();
		Set<S> services = new HashSet<>();

		while ( iterator.hasNext() ) {
			services.add( iterator.next() );
		}

		return services;
	}

}
