/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.environment.classpath.spi;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * Default implementation of {@code ClassResolver} relying on an {@link AggregatedClassLoader}.
 *
 * @author Hardy Ferentschik
 */
public final class DefaultClassResolver implements ClassResolver {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final AggregatedClassLoader aggregatedClassLoader;

	/**
	 * Constructs a ClassLoaderServiceImpl with standard set-up
	 */
	public DefaultClassResolver(AggregatedClassLoader aggregatedClassLoader) {
		this.aggregatedClassLoader = aggregatedClassLoader;
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public <T> Class<T> classForName(String className) {
		try {
			return (Class<T>) Class.forName( className, true, aggregatedClassLoader );
		}
		catch (Exception | LinkageError e) {
			throw log.unableToLoadTheClass( className, e );
		}
	}

}
