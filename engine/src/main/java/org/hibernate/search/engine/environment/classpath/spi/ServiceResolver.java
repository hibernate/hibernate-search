/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.environment.classpath.spi;

/**
 * A resolver of Java services.
 */
public interface ServiceResolver {
	/**
	 * Discovers and instantiates implementations of the named service contract.
	 * <p>
	 * NOTE : We are talking about services as defined by {@link java.util.ServiceLoader}.
	 *
	 * @param serviceContract The java type defining the service contract
	 * @param <T> The type of the service contract
	 *
	 * @return The ordered set of discovered services.
	 */
	<T> Iterable<T> loadJavaServices(Class<T> serviceContract);

	/**
	 * Discovers and instantiates jmx service of the named service contract.
	 * <p>
	 *
	 * @param serviceContract The java type defining the service contract
	 * @param <T> The type of the service contract
	 *
	 * @return The ordered set of discovered services.
	 */
	<T> T loadJmxService(Class<T> serviceContract);
}
