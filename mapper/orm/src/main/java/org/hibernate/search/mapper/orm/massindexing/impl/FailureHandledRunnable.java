/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.massindexing.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * Common parent of all Runnable implementations for the batch indexing:
 * share the code for handling runtime exceptions.
 */
abstract class FailureHandledRunnable implements Runnable {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final FailureHandler failureHandler;

	protected FailureHandledRunnable(FailureHandler failureHandler) {
		this.failureHandler = failureHandler;
	}

	@Override
	public final void run() {
		try {
			runWithFailureHandler();
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

			failureHandler.handleException( errorMessage , re );
		}
	}

	protected abstract void runWithFailureHandler() throws Exception;

	protected FailureHandler getFailureHandler() {
		return failureHandler;
	}

	protected void cleanUpOnError() {
		//no-op unless overridden
	}

}
