/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.schema.impl;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class ValidationErrorCollector {

	private final Deque<ValidationContextElement> currentContext = new ArrayDeque<ValidationContextElement>();

	private final Map<ValidationContext, List<String>> messagesByContext = new LinkedHashMap<>();

	public void push(ValidationContextType contextType, String name) {
		this.currentContext.addLast( new ValidationContextElement( contextType, name ) );
	}

	public void pop() {
		this.currentContext.removeLast();
	}

	public void addError(String errorMessage) {
		ValidationContext context = new ValidationContext( currentContext );
		List<String> messages = messagesByContext.get( context );
		if ( messages == null ) {
			messages = new ArrayList<>();
			messagesByContext.put( context, messages );
		}
		messages.add( errorMessage );
	}

	/**
	 * @return The collected messages mapped by their context.
	 */
	public Map<ValidationContext, List<String>> getMessagesByContext() {
		return messagesByContext;
	}
}