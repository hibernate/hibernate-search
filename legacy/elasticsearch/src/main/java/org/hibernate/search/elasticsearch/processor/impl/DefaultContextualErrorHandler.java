/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.processor.impl;

import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.elasticsearch.work.impl.ElasticsearchWork;
import org.hibernate.search.exception.ErrorHandler;
import org.hibernate.search.exception.impl.ErrorContextBuilder;

/**
 * Registers the errors during an execution ultimately passes them to an error handler.
 * <p>
 * This context is mutable and is not thread-safe.
 *
 * @author Yoann Rodiere
 */
class DefaultContextualErrorHandler implements ContextualErrorHandler {

	private final ErrorHandler errorHandler;

	private ErrorContextBuilder errorContextBuilder;

	private Throwable errorThatOccurred;

	public DefaultContextualErrorHandler(ErrorHandler errorHandler) {
		super();
		this.errorHandler = errorHandler;
	}

	@Override
	public void markAsFailed(ElasticsearchWork<?> work, Throwable throwable) {
		LuceneWork luceneWork = work.getLuceneWork();
		if ( luceneWork != null ) {
			getErrorContextBuilder().operationAtFault( luceneWork );
			getErrorContextBuilder().addWorkThatFailed( luceneWork );
		}
		addThrowable( throwable );
	}

	@Override
	public void markAsSkipped(ElasticsearchWork<?> work) {
		LuceneWork luceneWork = work.getLuceneWork();
		if ( luceneWork != null ) {
			getErrorContextBuilder().addWorkThatFailed( luceneWork );
		}
	}

	@Override
	public void addThrowable(Throwable throwable) {
		if ( errorThatOccurred == null ) {
			errorThatOccurred = throwable;
		}
		else {
			errorThatOccurred.addSuppressed( throwable );
		}
	}

	@Override
	public void handle() {
		if ( errorContextBuilder != null || errorThatOccurred != null ) {
			if ( errorThatOccurred != null ) {
				getErrorContextBuilder().errorThatOccurred( errorThatOccurred );
			}
			errorHandler.handle( getErrorContextBuilder().createErrorContext() );
		}
	}

	private ErrorContextBuilder getErrorContextBuilder() {
		if ( errorContextBuilder == null ) {
			errorContextBuilder = new ErrorContextBuilder();
		}
		return errorContextBuilder;
	}

}
