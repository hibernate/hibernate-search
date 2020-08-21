/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.errorhandling;

import org.hibernate.search.exception.ErrorContext;
import org.hibernate.search.exception.impl.LogErrorHandler;
import org.hibernate.search.indexes.spi.IndexManager;

/**
 * This is a LogErrorHandler used for testing only,
 * NOT to be used as a template or example for a real
 * error handler.
 *
 * @author Sanne Grinovero
 * @since 3.2
 */
public class MockErrorHandler extends LogErrorHandler {

	private volatile String errorMessage;
	private volatile Throwable lastException;
	private IndexManager indexManager;

	@Override
	public void handle(ErrorContext context) {
		indexManager = context.getIndexManager();
		super.handle( context );
	}

	@Override
	public void handleException(String errorMsg, Throwable exceptionThatOccurred) {
		errorMessage = errorMsg;
		lastException = exceptionThatOccurred;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public Throwable getLastException() {
		return lastException;
	}

	public IndexManager getIndexManager() {
		return indexManager;
	}

}
