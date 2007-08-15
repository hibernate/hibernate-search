//$Id$
package org.hibernate.search;

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