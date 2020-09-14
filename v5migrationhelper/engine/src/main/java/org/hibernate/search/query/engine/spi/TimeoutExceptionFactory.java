/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.query.engine.spi;

/**
 * The internal engine will use this exception factory to
 * throw exceptions of an appropriate type according to a
 * specific API.
 *
 * @author Sanne Grinovero (C) 2011 Red Hat Inc.
 */
public interface TimeoutExceptionFactory {

	RuntimeException createTimeoutException(String message, String queryDescription);

}
