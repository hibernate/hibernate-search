/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.batchindexing.impl;

import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.exception.ErrorHandler;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Common parent of all Runnable implementations for the batch indexing:
 * share the code for handling runtime exceptions.
 */
abstract class ErrorHandledRunnable implements Runnable {

	private static final Log log = LoggerFactory.make();

	protected final ExtendedSearchIntegrator extendedIntegrator;

	protected ErrorHandledRunnable(ExtendedSearchIntegrator extendedIntegrator) {
		this.extendedIntegrator = extendedIntegrator;
	}

	@Override
	public final void run() {
		ErrorHandler errorHandler = extendedIntegrator.getErrorHandler();
		try {
			runWithErrorHandler();
		}
		catch (Exception re) {
			//being this an async thread we want to make sure everything is somehow reported
			errorHandler.handleException( log.massIndexerUnexpectedErrorMessage() , re );
			cleanUpOnError();
		}
	}

	protected abstract void runWithErrorHandler() throws Exception;

	protected void cleanUpOnError() {
		//no-op unless overridden
	}

}
