/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.genericjpa.exception;

/**
 * Base Exception for Hibernate Search Generic JPA
 *
 * @author Martin Braun
 */
public class SearchException extends RuntimeException {

	private static final long serialVersionUID = 8364605079362949027L;

	public SearchException() {
		super();
	}

	public SearchException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super( message, cause, enableSuppression, writableStackTrace );
	}

	public SearchException(String message, Throwable cause) {
		super( message, cause );
	}

	public SearchException(String message) {
		super( message );
	}

	public SearchException(Throwable cause) {
		super( cause );
	}

}
