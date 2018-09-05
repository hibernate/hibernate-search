/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
/**
 * Classes offering a service infrastructure for Search.
 *
 * Entry point is the {@code ServiceManager} which allows to retrieve and release services. Services can be provided
 * programmatically or discovered via Java's {@link java.util.ServiceLoader} mechanism.
 *
 * In order to be a service an interface must extend the {@code Service} interface.
 * Optionally a service implementation can also implement {@code Startable} and/or {@code Stoppable} in order to get life cycle callbacks.
 *
 * @author Hardy Ferentschik
 */
package org.hibernate.search.engine.service.spi;
