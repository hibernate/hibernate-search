/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.environment.classpath.spi;

import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.annotation.impl.SuppressForbiddenApis;

/**
 * Indicates a problem performing class loading.
 *
 * @author Steve Ebersole
 * @author Hardy Ferentschik
 */
public class ClassLoadingException extends SearchException {
	/**
	 * Constructs a {@code ClassLoadingException} using the specified message and cause.
	 *
	 * @param message A message explaining the exception condition.
	 * @param cause The underlying cause
	 */
	@SuppressForbiddenApis(reason = SEARCH_EXCEPTION_AND_SUBCLASSES_CAN_USE_CONSTRUCTOR)
	public ClassLoadingException(String message, Throwable cause) {
		super( message, cause );
	}
}

