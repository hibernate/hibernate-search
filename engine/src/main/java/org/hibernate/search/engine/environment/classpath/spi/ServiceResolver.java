/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
}
