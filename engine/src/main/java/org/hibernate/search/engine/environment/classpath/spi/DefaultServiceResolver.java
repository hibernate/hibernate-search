/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.environment.classpath.spi;

import java.util.LinkedHashSet;
import java.util.ServiceLoader;
import java.util.Set;

/**
 * Default implementation of {@code ClassResolver} relying on an {@link AggregatedClassLoader}.
 */
public final class DefaultServiceResolver implements ServiceResolver {

	private final AggregatedClassLoader aggregatedClassLoader;

	public static ServiceResolver create(AggregatedClassLoader aggregatedClassLoader) {
		return new DefaultServiceResolver( aggregatedClassLoader );
	}

	private DefaultServiceResolver(AggregatedClassLoader aggregatedClassLoader) {
		this.aggregatedClassLoader = aggregatedClassLoader;
	}

	@Override
	public <S> Set<S> loadJavaServices(Class<S> serviceContract) {
		ServiceLoader<S> serviceLoader = ServiceLoader.load( serviceContract, aggregatedClassLoader );
		final LinkedHashSet<S> services = new LinkedHashSet<>();
		for ( S service : serviceLoader ) {
			services.add( service );
		}
		return services;
	}
}
