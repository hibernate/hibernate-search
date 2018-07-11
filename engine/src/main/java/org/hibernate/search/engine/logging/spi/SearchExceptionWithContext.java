/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.logging.spi;

import org.hibernate.search.util.SearchException;

public class SearchExceptionWithContext extends SearchException {
	private final String messageWithoutContext;
	private final FailureContext context;

	public SearchExceptionWithContext(String message, Throwable cause, FailureContext context) {
		super( message + "\n" + context.render(), cause );
		this.messageWithoutContext = message;
		this.context = context;
	}

	public SearchExceptionWithContext(String message, FailureContext context) {
		this( message, null, context );
	}

	public SearchExceptionWithContext(Throwable cause, FailureContext context) {
		this( null, cause, context );
	}

	public String getMessageWithoutContext() {
		return messageWithoutContext;
	}

	public FailureContext getContext() {
		return context;
	}
}
