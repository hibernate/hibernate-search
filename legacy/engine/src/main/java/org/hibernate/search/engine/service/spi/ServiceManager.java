/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.service.spi;

import org.hibernate.search.engine.service.beanresolver.spi.BeanResolver;
import org.hibernate.search.engine.service.classloading.spi.ClassLoaderService;

/**
 * The {@code ServiceManager} is used to manage services in and runtime discovery of service implementations in the scope
 * of a single {@code SearchFactory}.
 * <p>
 * Services are divided into discovered services (via {@link java.util.ServiceLoader} and provided services. The latter occurs
 * via {@link org.hibernate.search.cfg.spi.SearchConfiguration#getProvidedServices()} and
 * {@link org.hibernate.search.cfg.spi.SearchConfiguration#getClassLoaderService()}.
 * </p>
 * <p>
 * It is the responsibility of the provider of these services to manage their life cycle: even if they implement
 * {@link org.hibernate.search.engine.service.spi.Startable} or {@link org.hibernate.search.engine.service.spi.Stoppable},
 * they will not be started or stopped automatically, while the methods on these interfaces will be invoked for
 * the services discovered via the serviceloader.
 * </p>
 * <p>
 * Any service requested should be released using {@link #releaseService(Class)} when it's not needed anymore.
 * </p>
 *
 * @author Hardy Ferentschik
 */
public interface ServiceManager {

	/**
	 * Gets the service in the specified service role.
	 *
	 * @param <S> the type of the service
	 * @param serviceRole the service to retrieve. Cannot be {@code null}.
	 *
	 * @return the single service fulfilling the specified role.
	 *
	 * @throws IllegalArgumentException in case the {@code serviceRole} is {@code null}
	 * @throws org.hibernate.search.exception.SearchException in case no service fulfilling the role could be located
	 * @throws java.lang.IllegalStateException in case this method is called after {@link #releaseService(Class)}
	 */
	<S extends Service> S requestService(Class<S> serviceRole);

	/**
	 * Gets a reference to the service with the requested role.
	 *
	 * @param <S> the type of the service
	 * @param serviceRole the service to retrieve. Cannot be {@code null}.
	 *
	 * @return the single service fulfilling the specified role.
	 *
	 * @throws IllegalArgumentException in case the {@code serviceRole} is {@code null}
	 * @throws org.hibernate.search.exception.SearchException in case no service fulfilling the role could be located
	 * @throws java.lang.IllegalStateException in case this method is called after {@link #releaseService(Class)}
	 */
	<S extends Service> ServiceReference<S> requestReference(Class<S> serviceRole);

	/**
	 * Releases the service in the specified service role.
	 *
	 * @param <S> the type of the service
	 * @param serviceRole the service to be released. Cannot be {@code null}.
	 *
	 * @throws IllegalArgumentException in case the {@code serviceRole} is {@code null}
	 */
	<S extends Service> void releaseService(Class<S> serviceRole);

	/**
	 * Stops and releases all services. After this method is called no further services can be requested. An
	 * {@code IllegalStateException} will be thrown in this case.
	 */
	void releaseAllServices();

	/**
	 * Provides direct access to the {@link ClassLoaderService}.
	 * This service lookup is treated as a special case both for convenience and performance reasons.
	 * @return the {@link ClassLoaderService}
	 */
	ClassLoaderService getClassLoaderService();

	/**
	 * Provides direct access to the {@link BeanResolver}.
	 * This service lookup is treated as a special case for convenience.
	 * @return the {@link BeanResolver}
	 */
	BeanResolver getBeanResolver();

}
