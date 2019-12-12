/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.common;


/**
 * Represent a timeout during a Hibernate Search operation.
 */
public class SearchTimeoutException extends SearchException {

	public SearchTimeoutException(String message, Throwable cause) {
		super( message, cause );
	}

	public SearchTimeoutException(String message) {
		super( message );
	}

}


