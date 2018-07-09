/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.logging.spi;

import java.util.List;

import org.hibernate.search.util.SearchException;

public class SearchExceptionWithContext extends SearchException {
	private final String messageWithoutContext;
	private final List<FailureContextElement> contextElements;

	public SearchExceptionWithContext(String message, Throwable cause, List<FailureContextElement> contextElements) {
		super( message + FailureContexts.renderForException( contextElements ), cause );
		this.messageWithoutContext = message;
		this.contextElements = contextElements;
	}

	public SearchExceptionWithContext(String message, List<FailureContextElement> contextElements) {
		this( message, null, contextElements );
	}

	public SearchExceptionWithContext(Throwable cause, List<FailureContextElement> contextElements) {
		this( null, cause, contextElements );
	}

	public String getMessageWithoutContext() {
		return messageWithoutContext;
	}

	public List<FailureContextElement> getContextElements() {
		return contextElements;
	}
}
