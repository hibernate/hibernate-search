/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.orchestration.impl;

import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchWork;
import org.hibernate.search.engine.common.spi.ErrorContextBuilder;
import org.hibernate.search.engine.common.spi.ErrorHandler;

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
		Object info = work.getInfo();
		if ( info != null ) {
			getErrorContextBuilder().operationAtFault( info );
			getErrorContextBuilder().addWorkThatFailed( info );
		}
		addThrowable( throwable );
	}

	@Override
	public void markAsSkipped(ElasticsearchWork<?> work) {
		Object info = work.getInfo();
		if ( info != null ) {
			getErrorContextBuilder().addWorkThatFailed( info );
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
