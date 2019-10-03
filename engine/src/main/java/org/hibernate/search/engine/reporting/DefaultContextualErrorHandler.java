/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.reporting;

import org.hibernate.search.engine.reporting.spi.ErrorContextBuilder;

/**
 * Registers the errors during an execution ultimately passes them to an error handler.
 * <p>
 * This context is mutable and is not thread-safe.
 *
 */
class DefaultContextualErrorHandler implements ContextualErrorHandler {

	private final ErrorHandler errorHandler;

	private ErrorContextBuilder errorContextBuilder;

	private Throwable errorThatOccurred;

	DefaultContextualErrorHandler(ErrorHandler errorHandler) {
		this.errorHandler = errorHandler;
	}

	@Override
	public void markAsFailed(Object workInfo, Throwable throwable) {
		if ( workInfo != null ) {
			getErrorContextBuilder().operationAtFault( workInfo );
			getErrorContextBuilder().addWorkThatFailed( workInfo );
		}
		addThrowable( throwable );
	}

	@Override
	public void markAsSkipped(Object workInfo) {
		if ( workInfo != null ) {
			getErrorContextBuilder().addWorkThatFailed( workInfo );
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
