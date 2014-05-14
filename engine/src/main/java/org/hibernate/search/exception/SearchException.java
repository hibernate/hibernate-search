/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.exception;

/**
 * Root of all search specific exceptions
 *
 * @author Emmanuel Bernard
 */
public class SearchException extends RuntimeException {

	public SearchException() {
		super();
	}

	public SearchException(String message) {
		super( message );
	}

	public SearchException(String message, Throwable cause) {
		super( message, cause );
	}

	public SearchException(Throwable cause) {
		super( cause );
	}
}
