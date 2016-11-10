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

import org.hibernate.search.util.StringHelper;

final class ValidationErrorCollector {

	private String indexName;
	private String mappingName;
	private final Deque<String> currentPath = new ArrayDeque<String>();

	private final Map<ValidationContext, List<String>> messagesByContext = new LinkedHashMap<>();

	public void setIndexName(String indexName) {
		this.indexName = indexName;
	}

	public void setMappingName(String mappingName) {
		this.mappingName = mappingName;
	}

	public void pushPropertyName(String propertyName) {
		currentPath.addLast( propertyName );
	}

	public void popPropertyName() {
		currentPath.removeLast();
	}

	public void addError(String errorMessage) {
		ValidationContext context = createContext();
		List<String> messages = messagesByContext.get( context );
		if ( messages == null ) {
			messages = new ArrayList<>();
			messagesByContext.put( context, messages );
		}
		messages.add( errorMessage );
	}

	private ValidationContext createContext() {
		return new ValidationContext( indexName, mappingName, StringHelper.join( currentPath, "." ) );
	}

	/**
	 * @return The collected messages mapped by their context.
	 */
	public Map<ValidationContext, List<String>> getMessagesByContext() {
		return messagesByContext;
	}
}