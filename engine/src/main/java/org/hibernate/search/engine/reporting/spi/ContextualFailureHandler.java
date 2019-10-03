/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.reporting.spi;

import org.hibernate.search.engine.reporting.FailureHandler;

/**
 * Registers the failures during an execution ultimately passes them to a failure handler.
 * <p>
 * This context is mutable and is not thread-safe.
 */
public class ContextualFailureHandler {

	private final FailureHandler failureHandler;

	private IndexFailureContextImpl.Builder failureContextBuilder;

	private Throwable errorThatOccurred;

	public ContextualFailureHandler(FailureHandler failureHandler) {
		this.failureHandler = failureHandler;
	}

	public void markAsFailed(Object workInfo, Throwable throwable) {
		if ( workInfo != null ) {
			getFailureContextBuilder().failingOperation( workInfo );
			getFailureContextBuilder().uncommittedOperation( workInfo );
		}
		addThrowable( throwable );
	}

	public void markAsSkipped(Object workInfo) {
		if ( workInfo != null ) {
			getFailureContextBuilder().uncommittedOperation( workInfo );
		}
	}

	public void addThrowable(Throwable throwable) {
		if ( errorThatOccurred == null ) {
			errorThatOccurred = throwable;
		}
		else {
			errorThatOccurred.addSuppressed( throwable );
		}
	}

	public void handle() {
		if ( failureContextBuilder != null || errorThatOccurred != null ) {
			if ( errorThatOccurred != null ) {
				getFailureContextBuilder().throwable( errorThatOccurred );
			}
			failureHandler.handle( getFailureContextBuilder().build() );
		}
	}

	private IndexFailureContextImpl.Builder getFailureContextBuilder() {
		if ( failureContextBuilder == null ) {
			failureContextBuilder = new IndexFailureContextImpl.Builder();
		}
		return failureContextBuilder;
	}

}
