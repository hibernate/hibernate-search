/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.common;

import org.hibernate.search.util.common.reporting.EventContext;

public class SearchException extends RuntimeException {
	private final String messageWithoutContext;
	private final EventContext context;

	public SearchException(String message, Throwable cause) {
		super( message, cause );
		this.messageWithoutContext = message;
		this.context = null;
	}

	public SearchException(String message) {
		this( message, (Throwable) null );
	}

	public SearchException(Throwable cause) {
		this( null, cause );
	}

	public SearchException(String message, Throwable cause, EventContext context) {
		super( context == null ? message : message + "\n" + context.renderWithPrefix(), cause );
		this.messageWithoutContext = message;
		this.context = context;
	}

	public SearchException(String message, EventContext context) {
		this( message, null, context );
	}

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
	 * @deprecated Use {@link #messageWithoutContext} instead.
	 * @return The exception message, without the description of the context.
	 */
	@Deprecated
	public String getMessageWithoutContext() {
		return messageWithoutContext();
	}

	/**
	 * @return The context in which this exception occurred.
	 */
	public EventContext context() {
		return context;
	}

	/**
	 * @deprecated Use {@link #context} instead.
	 * @return The context in which this exception occurred.
	 */
	@Deprecated
	public EventContext getContext() {
		return context();
	}
}
