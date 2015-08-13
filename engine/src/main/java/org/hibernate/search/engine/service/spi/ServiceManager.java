/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.service.spi;

/**
 * The {@code ServiceManager} is used to manage services in and runtime discovery of service implementations in the scope
 * of a single {@code SearchFactory}.
 * <p>
 * Services are divided into discovered services (via {@link java.util.ServiceLoader} and provided services. The latter occurs
 * via {@link org.hibernate.search.cfg.spi.SearchConfiguration#getProvidedServices()} and
 * {@link org.hibernate.search.cfg.spi.SearchConfiguration#getClassLoaderService()}. Provided services are also treated
 * special in the sense that they are not allowed to implemented {@link org.hibernate.search.engine.service.spi.Startable} or
 * {@link org.hibernate.search.engine.service.spi.Stoppable} (an exception is thrown if they do so).
 * It is the responsibility of the provider of these services to manage their life cycle. This also prevents circular
 * dependencies where a service where a service during bootstrapping could request other (uninitialized) services via
 * the {@link org.hibernate.search.engine.service.spi.Startable#start(java.util.Properties, org.hibernate.search.spi.BuildContext)}
 * callback.
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
}
