/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.environment.classpath.spi;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * Default implementation of {@code ClassResolver} relying on an {@link AggregatedClassLoader}.
 */
public abstract class DefaultServiceResolver implements ServiceResolver {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );
	private static final Method SERVICE_LOADER_STREAM_METHOD;
	private static final Method PROVIDER_TYPE_METHOD;

	static {
		Class<?> serviceLoaderClass = ServiceLoader.class;
		Method serviceLoaderStreamMethod = null;
		Method providerTypeMethod = null;
		try {
			/*
			 * JDK 9 introduced the stream() method on ServiceLoader,
			 * which we need in order to avoid duplicate service instantiation.
			 * See ClassPathAndModulePathServiceResolver.
			 */
			serviceLoaderStreamMethod = serviceLoaderClass.getMethod( "stream" );
			Class<?> providerClass = Class.forName( serviceLoaderClass.getName() + "$Provider" );
			providerTypeMethod = providerClass.getMethod( "type" );
		}
		catch (NoSuchMethodException | ClassNotFoundException e) {
			/*
			 * Probably Java 8.
			 * Leave the method constants null,
			 * we will automatically use a service loader implementation that doesn't rely on them.
			 * See create(...).
			 */
		}

		SERVICE_LOADER_STREAM_METHOD = serviceLoaderStreamMethod;
		PROVIDER_TYPE_METHOD = providerTypeMethod;
	}

	public static ServiceResolver create(AggregatedClassLoader aggregatedClassLoader) {
		if ( SERVICE_LOADER_STREAM_METHOD != null ) {
			// Java 9+
			return new ClassPathAndModulePathServiceResolver( aggregatedClassLoader );
		}
		else {
			// Java 8
			return new ClassPathOnlyServiceResolver( aggregatedClassLoader );
		}
	}

	final AggregatedClassLoader aggregatedClassLoader;

	private DefaultServiceResolver(AggregatedClassLoader aggregatedClassLoader) {
		this.aggregatedClassLoader = aggregatedClassLoader;
	}

	/**
	 * A {@link ServiceResolver} that will only detect services defined in the class path,
	 * because it passes the aggregated classloader directly to the service loader.
	 * <p>
	 * This implementation is best when running Hibernate Search on Java 8.
	 * On Java 9 and above, {@link ClassPathAndModulePathServiceResolver} should be used.
	 */
	private static class ClassPathOnlyServiceResolver extends DefaultServiceResolver {

		private ClassPathOnlyServiceResolver(AggregatedClassLoader aggregatedClassLoader) {
			super( aggregatedClassLoader );
		}

		@Override
		public <S> Set<S> loadJavaServices(Class<S> serviceContract) {
			ServiceLoader<S> serviceLoader = ServiceLoader.load( serviceContract, aggregatedClassLoader );
			final Set<S> services = new LinkedHashSet<>();
			for ( S service : serviceLoader ) {
				services.add( service );
			}
			return services;
		}
	}

	/**
	 * A {@link ServiceResolver} that will detect services defined in the class path or in the module path.
	 * <p>
	 * This implementation only works when running Hibernate Search on Java 9 and above.
	 * On Java 8, {@link ClassPathOnlyServiceResolver} must be used.
	 * <p>
	 * When retrieving services from providers in the module path,
	 * the service loader internally uses a map from classloader to service catalog.
	 * Since the aggregated class loader is artificial and unknown from the service loader,
	 * it will never match any service from the module path.
	 * <p>
	 * To work around this problem,
	 * we try to get services from a service loader bound to the aggregated class loader first,
	 * then we try a service loader bound to each individual class loader.
	 * <p>
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
	private static class ClassPathAndModulePathServiceResolver extends DefaultServiceResolver {
		private final List<ClassLoader> classLoaders;

		private ClassPathAndModulePathServiceResolver(AggregatedClassLoader aggregatedClassLoader) {
			super( aggregatedClassLoader );
			this.classLoaders = new ArrayList<>();
			// Individual class loaders to try besides an aggregated class loader,
			// because only them can instantiate services provided by jars in the module path
			aggregatedClassLoader.addAllTo( this.classLoaders );
		}

		@Override
		public <S> Collection<S> loadJavaServices(Class<S> serviceContract) {
			final Set<String> alreadyUsedProviderTypes = new LinkedHashSet<>();
			final Set<S> services = new LinkedHashSet<>();

			// Always try the aggregated class loader first and never ignore any errors it might produce:
			Iterator<? extends Supplier<S>> iterator = providerIterator( aggregatedClassLoader, serviceContract );
			while ( iterator.hasNext() ) {
				collectServiceIfNotDuplicate( services, alreadyUsedProviderTypes, iterator.next() );
			}

			// Then go through other classloaders, but ignore service configuration errors.
			// Could happen for example in quarkus with AppClassLoader, but those services were already loaded by either aggregated or other classloaders.
			for ( ClassLoader loader : classLoaders ) {
				iterator = providerIterator( loader, serviceContract );

				while ( hasNextIgnoringServiceConfigurationError( iterator, serviceContract ) ) {
					collectServiceIfNotDuplicate( services, alreadyUsedProviderTypes, iterator.next() );
				}
			}

			return services;
		}

		@SuppressWarnings("unchecked")
		private <S> Iterator<? extends Supplier<S>> providerIterator(ClassLoader classLoader, Class<S> serviceContract) {
			try {
				ServiceLoader<S> delegate = ServiceLoader.load( serviceContract, classLoader );
				return ((Stream<? extends Supplier<S>>) SERVICE_LOADER_STREAM_METHOD.invoke( delegate )).iterator();
			}
			catch (RuntimeException | IllegalAccessException | InvocationTargetException e) {
				throw new AssertionFailure( "Error calling ServiceLoader.stream()", e );
			}
		}

		/**
		 * Only adds a supplied provider if its type name is not present in `alreadyUsedProviderTypes`. Ignores it otherwise.
		 */
		private <S> void collectServiceIfNotDuplicate(Set<S> services, Set<String> alreadyUsedProviderTypes, Supplier<S> provider) {
			Class<?> type;
			try {
				type = (Class<?>) PROVIDER_TYPE_METHOD.invoke( provider );
			}
			catch (RuntimeException | IllegalAccessException | InvocationTargetException e) {
				throw new AssertionFailure( "Error calling ServiceLoader.Provider.type()", e );
			}
			String typeName = type.getName();
			/*
			 * We may encounter service provider multiple times,
			 * because the individual classloaders may give access to the same types.
			 * However, we only want to get the service from each provider once.
			 *
			 * ServiceLoader.stream() is useful in that regard,
			 * since it allows us to check the type of the service provider
			 * before the service is even instantiated.
			 *
			 * We could just instantiate every service and check their type afterwards,
			 * but 1. it would lead to unnecessary instantiation which could have side effects,
			 * in particular regarding class loading,
			 * and 2. the type of the provider may not always be the type of the service,
			 * and one provider may return different types of services
			 * depending on conditions known only to itself.
			 */
			if ( alreadyUsedProviderTypes.add( typeName ) ) {
				services.add( provider.get() );
			}
		}

		/**
		 * Skips any service configuration errors when trying to move to the next service supplier.
		 */
		private boolean hasNextIgnoringServiceConfigurationError(Iterator<?> iterator, Class<?> serviceContract) {
			while ( true ) {
				try {
					return iterator.hasNext();
				}
				catch (ServiceConfigurationError e) {
					log.ignoringServiceConfigurationError( serviceContract, e );
				}
			}
		}
	}
}
