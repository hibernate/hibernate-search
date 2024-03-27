/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.common;

import org.hibernate.search.util.common.annotation.impl.SuppressForbiddenApis;
import org.hibernate.search.util.common.reporting.EventContext;

public class SearchException extends RuntimeException {
	protected static final String SEARCH_EXCEPTION_AND_SUBCLASSES_CAN_USE_CONSTRUCTOR =
			"SearchException and its subclasses are allowed to use SearchException constructors"
					+ " without delegating to Jboss-Logging.";

	private final String messageWithoutContext;
	private final EventContext context;

	public SearchException(String message, Throwable cause) {
		super( message, cause );
		this.messageWithoutContext = message;
		this.context = null;
	}

	@SuppressForbiddenApis(reason = SEARCH_EXCEPTION_AND_SUBCLASSES_CAN_USE_CONSTRUCTOR)
	public SearchException(String message) {
		this( message, (Throwable) null );
	}

	@SuppressForbiddenApis(reason = SEARCH_EXCEPTION_AND_SUBCLASSES_CAN_USE_CONSTRUCTOR)
	public SearchException(Throwable cause) {
		this( null, cause );
	}

	@SuppressForbiddenApis(reason = SEARCH_EXCEPTION_AND_SUBCLASSES_CAN_USE_CONSTRUCTOR)
	public SearchException(String message, Throwable cause, EventContext context) {
		super( context == null ? message : message + "\n" + context.renderWithPrefix(), cause );
		this.messageWithoutContext = message;
		this.context = context;
	}

	@SuppressForbiddenApis(reason = SEARCH_EXCEPTION_AND_SUBCLASSES_CAN_USE_CONSTRUCTOR)
	public SearchException(String message, EventContext context) {
		this( message, null, context );
	}

	@SuppressForbiddenApis(reason = SEARCH_EXCEPTION_AND_SUBCLASSES_CAN_USE_CONSTRUCTOR)
	public SearchException(Throwable cause, EventContext context) {
		this( null, cause, context );
	}

	/**
	 * @return The exception message, without the description of the context.
	 */
	public String messageWithoutContext() {
		return messageWithoutContext;
	}

	/**
	 * @return The context in which this exception occurred.
	 */
	public EventContext context() {
		return context;
	}
}
