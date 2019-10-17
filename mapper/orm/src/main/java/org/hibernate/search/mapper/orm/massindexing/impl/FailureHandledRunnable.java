/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.massindexing.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.engine.reporting.FailureContext;
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
		boolean interrupted = false;
		try {
			runWithFailureHandler();
		}
		catch (InterruptedException e) {
			interrupted = true;
			try {
				cleanUpOnInterruption();
			}
			catch (RuntimeException | InterruptedException e2) {
				e.addSuppressed( e2 );
			}

			notifyInterrupted( e );
		}
		catch (RuntimeException e) {
			try {
				cleanUpOnFailure();
			}
			catch (RuntimeException e2) {
				e.addSuppressed( e2 );
			}
			catch (InterruptedException e2) {
				interrupted = true;
				e.addSuppressed( e2 );
			}

			notifyFailure( e );
		}
		finally {
			if ( interrupted ) {
				// Restore interruption signal
				Thread.currentThread().interrupt();
			}
		}
	}

	protected abstract void runWithFailureHandler() throws InterruptedException;

	protected abstract void cleanUpOnInterruption() throws InterruptedException;

	protected abstract void cleanUpOnFailure() throws InterruptedException;

	protected final FailureHandler getFailureHandler() {
		return failureHandler;
	}

	protected void notifyInterrupted(InterruptedException exception) {
		FailureContext.Builder contextBuilder = FailureContext.builder();
		contextBuilder.throwable( log.massIndexingThreadInterrupted( exception ) );
		contextBuilder.failingOperation( log.massIndexerOperation() );
		failureHandler.handle( contextBuilder.build() );
	}

	protected void notifyFailure(RuntimeException exception) {
		FailureContext.Builder contextBuilder = FailureContext.builder();
		contextBuilder.throwable( exception );
		contextBuilder.failingOperation( log.massIndexerOperation() );
		failureHandler.handle( contextBuilder.build() );
	}

}
