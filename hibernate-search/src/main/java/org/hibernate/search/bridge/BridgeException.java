package org.hibernate.search.bridge;

import org.hibernate.search.SearchException;

/**
 * @author Emmanuel Bernard
 */
public class BridgeException extends SearchException {
	public BridgeException() {
		super();
	}

	public BridgeException(String message) {
		super(message);
	}

	public BridgeException(String message, Throwable cause) {
		super(message, cause);
	}

	public BridgeException(Throwable cause) {
		super(cause);
	}
}
