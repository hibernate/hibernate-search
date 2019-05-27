/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.environment.classpath.spi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;

/**
 * Default implementation of {@code ClassResolver} relying on an {@link AggregatedClassLoader}.
 * <p>
 * When retrieving services from providers in the module path,
 * the service loader internally uses a map from classloader to service catalog.
 * Since the aggregated class loader is artificial and unknown from the service loader,
 * it will never match any service from the module path.
 * <p>
 * To work around this problem,
 * we instantiate one service loader per individual class loader and get services from these.
 * This could result in duplicates, so we take specific care to avoid using the same service provider twice.
 * See {@link #loadJavaServices(Class)}.
 * <p>
 * Note that, in the worst case,
 * the service retrieved from each individual class loader could load duplicate versions
 * of classes already loaded from another class loader.
 * For example in an aggregated class loader made up of individual class loader A, B, C:
 * it is possible that class C1 was already loaded from A,
 * but then we load service S1 from B, and this service will also need class C1 but won't find it in class loader B,
 * so it will load its own version of that class.
 * <p>
 * We assume that this situation will never occur in practice because class loaders
 * are structure in a hierarchy that prevents one class to be loaded twice.
 */
public final class DefaultServiceResolver implements ServiceResolver {

	public static ServiceResolver create(AggregatedClassLoader aggregatedClassLoader) {
		return new DefaultServiceResolver( aggregatedClassLoader );
	}

	private final List<ClassLoader> classLoaders;

	private DefaultServiceResolver(AggregatedClassLoader aggregatedClassLoader) {
		this.classLoaders = new ArrayList<>();
		// Always try the aggregated class loader first
		this.classLoaders.add( aggregatedClassLoader );
		// Then also try the individual class loaders,
		// because only them can instantiate services provided by jars in the module path
		aggregatedClassLoader.addAllTo( this.classLoaders );
	}

	@Override
	public <S> Collection<S> loadJavaServices(Class<S> serviceContract) {
		Set<String> alreadyEncountered = new HashSet<>();
		Set<S> result = new LinkedHashSet<>();
		for ( ClassLoader classLoader : classLoaders ) {
			ServiceLoader<S> serviceLoader = ServiceLoader.load( serviceContract, classLoader );
			for ( S service : serviceLoader ) {
				Class<?> type = service.getClass();
				String typeName = type.getName();
				/*
				 * We may encounter the same service provider multiple times,
				 * because the individual class loaders may give access to the same types
				 * (at the very least a single class loader may be present twice in the aggregated class loader).
				 * However, we only want to get the service from each provider once.
				 */
				if ( alreadyEncountered.add( typeName ) ) {
					result.add( service );
				}
			}
		}
		return result;
	}
}
