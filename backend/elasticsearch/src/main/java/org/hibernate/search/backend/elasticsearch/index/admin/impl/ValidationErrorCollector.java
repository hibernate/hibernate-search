/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.index.admin.impl;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

import org.hibernate.search.engine.logging.spi.ContextualFailureCollector;
import org.hibernate.search.engine.logging.spi.EventContexts;
import org.hibernate.search.util.AssertionFailure;

final class ValidationErrorCollector {

	private final Deque<ValidationContextElement> currentContext = new ArrayDeque<>();

	private final ContextualFailureCollector failureCollector;

	private boolean hasError = false;

	ValidationErrorCollector() {
		this.failureCollector = null;
	}

	ValidationErrorCollector(ContextualFailureCollector failureCollector) {
		this.failureCollector = failureCollector;
	}

	void push(ValidationContextType contextType, String name) {
		this.currentContext.addLast( new ValidationContextElement( contextType, name ) );
	}

	void pop() {
		this.currentContext.removeLast();
	}

	void addError(String errorMessage) {
		if ( failureCollector != null ) {
			appendContext( failureCollector, currentContext ).add( errorMessage );
		}
		hasError = true;
	}

	boolean hasError() {
		return hasError;
	}

	/**
	 * Add the given validation context to the failure collector,
	 * and return the result.
	 * <p>
	 * Multiple consecutive property or field contexts are squeezed into a single path
	 * (e.g. "foo" followed by "bar" becomes "foo.bar".
	 *
	 * @param failureCollector The failure collector to append context to.
	 * @param contextElements The validation context elements to append.
	 * @return A failure collector with the given validation context appended.
	 */
	private ContextualFailureCollector appendContext(ContextualFailureCollector failureCollector,
			Iterable<ValidationContextElement> contextElements) {
		Iterator<ValidationContextElement> iterator = contextElements.iterator();
		if ( !iterator.hasNext() ) {
			return failureCollector;
		}

		ContextualFailureCollector result = failureCollector;
		StringBuilder pathBuilder = new StringBuilder();
		while ( iterator.hasNext() ) {
			result = appendContext( result, iterator.next(), pathBuilder, iterator.hasNext() );
		}
		return result;
	}

	private ContextualFailureCollector appendContext(ContextualFailureCollector currentResult,
			ValidationContextElement element, StringBuilder pathBuilder, boolean hasNext) {
		ValidationContextType type = element.getType();
		String name = element.getName();
		if ( ValidationContextType.MAPPING_PROPERTY.equals( type )
				|| ValidationContextType.MAPPING_PROPERTY_FIELD.equals( type ) ) {
			// Try to build as long a field path as we can, for the sake of brevity
			if ( pathBuilder.length() > 0 ) {
				pathBuilder.append( "." );
			}
			pathBuilder.append( name );
			if ( !hasNext ) {
				return currentResult.withContext( EventContexts.fromIndexFieldAbsolutePath( pathBuilder.toString() ) );
			}
			else {
				return currentResult;
			}
		}
		else {
			if ( pathBuilder.length() > 0 ) {
				// Flush the path before doing anything
				currentResult = currentResult.withContext( EventContexts.fromIndexFieldAbsolutePath( pathBuilder.toString() ) );
				pathBuilder.setLength( 0 );
			}
			switch ( type ) {
				case ANALYZER:
					return currentResult.withContext( EventContexts.fromAnalyzer( name ) );
				case NORMALIZER:
					return currentResult.withContext( EventContexts.fromNormalizer( name ) );
				case CHAR_FILTER:
					return currentResult.withContext( EventContexts.fromCharFilter( name ) );
				case TOKENIZER:
					return currentResult.withContext( EventContexts.fromTokenizer( name ) );
				case TOKEN_FILTER:
					return currentResult.withContext( EventContexts.fromTokenFilter( name ) );
				default:
					throw new AssertionFailure( "Unexpected validation context element type: " + type );
			}
		}
	}
}