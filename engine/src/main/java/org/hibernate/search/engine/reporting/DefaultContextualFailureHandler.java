/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.reporting;

import org.hibernate.search.engine.reporting.spi.FailureContextBuilder;

/**
 * Registers the failures during an execution ultimately passes them to a failure handler.
 * <p>
 * This context is mutable and is not thread-safe.
 */
class DefaultContextualFailureHandler implements ContextualFailureHandler {

	private final FailureHandler failureHandler;

	private FailureContextBuilder failureContextBuilder;

	private Throwable errorThatOccurred;

	DefaultContextualFailureHandler(FailureHandler failureHandler) {
		this.failureHandler = failureHandler;
	}

	@Override
	public void markAsFailed(Object workInfo, Throwable throwable) {
		if ( workInfo != null ) {
			getFailureContextBuilder().operationAtFault( workInfo );
			getFailureContextBuilder().addWorkThatFailed( workInfo );
		}
		addThrowable( throwable );
	}

	@Override
	public void markAsSkipped(Object workInfo) {
		if ( workInfo != null ) {
			getFailureContextBuilder().addWorkThatFailed( workInfo );
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
		if ( failureContextBuilder != null || errorThatOccurred != null ) {
			if ( errorThatOccurred != null ) {
				getFailureContextBuilder().throwable( errorThatOccurred );
			}
			failureHandler.handle( getFailureContextBuilder().createFailureContext() );
		}
	}

	private FailureContextBuilder getFailureContextBuilder() {
		if ( failureContextBuilder == null ) {
			failureContextBuilder = new FailureContextBuilder();
		}
		return failureContextBuilder;
	}

}
