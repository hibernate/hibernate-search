/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.bridge;

import org.hibernate.search.exception.SearchException;

/**
 * Exceptions thrown in a bridge are wrapped in BridgeException
 * The BridgeException message provides useful contextual information
 * to the developers.
 *
 * @author Emmanuel Bernard
 */
public class BridgeException extends SearchException {

	public BridgeException() {
		super();
	}

	public BridgeException(String message) {
		super( message );
	}

	public BridgeException(String message, Throwable cause) {
		super( message, cause );
	}

	public BridgeException(Throwable cause) {
		super( cause );
	}

}
