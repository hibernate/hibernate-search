/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.massindexing.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.engine.common.spi.ErrorHandler;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * Common parent of all Runnable implementations for the batch indexing:
 * share the code for handling runtime exceptions.
 */
abstract class ErrorHandledRunnable implements Runnable {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final ErrorHandler errorHandler;

	protected ErrorHandledRunnable(ErrorHandler errorHandler) {
		this.errorHandler = errorHandler;
	}

	@Override
	public final void run() {
		try {
			runWithErrorHandler();
		}
		catch (Exception re) {
			try {
				cleanUpOnError();
			}
			catch (RuntimeException e) {
				re.addSuppressed( e );
			}

			// being this an async thread we want to make sure everything is somehow reported
			String errorMessage = log.massIndexerUnexpectedErrorMessage();

			errorHandler.handleException( errorMessage , re );
		}
	}

	protected abstract void runWithErrorHandler() throws Exception;

	protected ErrorHandler getErrorHandler() {
		return errorHandler;
	}

	protected void cleanUpOnError() {
		//no-op unless overridden
	}

}
