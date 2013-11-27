/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
package org.hibernate.search.engine.service.spi;

/**
 * The {@code ServiceManager} is used to manage uniqueness of services and runtime discovery of service implementations.
 * <p/>
 * Uniqueness is meant in the scope of the {@code SearchFactory}, as there is a single {@code ServiceManager} instance
 * per {@code SearchFactory}.
 * <p/>
 * Any service requested should be released using {@link #releaseService(Class)} when it's not needed anymore.
 *
 * @author Hardy Ferentschik
 */
public interface ServiceManager {

	/**
	 * Gets the service in the specified service role.
	 *
	 * @param serviceRole the service to retrieve. Cannot be {@code null}.
	 *
	 * @return the single service fulfilling the specified role.
	 *
	 * @throws IllegalArgumentException in case the {@code serviceRole} is {@code null}
	 */
	<S extends Service> S requestService(Class<S> serviceRole);

	/**
	 * Releases the service in the specified service role.
	 *
	 * @param serviceRole the service to be released. Cannot be {@code null}.
	 *
	 * @throws IllegalArgumentException in case the {@code serviceRole} is {@code null}
	 */
	<S extends Service> void releaseService(Class<S> serviceRole);

	/**
	 * Stops all services.
	 */
	void releaseAllServices();
}
