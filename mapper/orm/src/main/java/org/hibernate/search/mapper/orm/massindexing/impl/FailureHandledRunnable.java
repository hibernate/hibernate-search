/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.massindexing.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * Common parent of all Runnable implementations for the batch indexing:
 * share the code for handling runtime exceptions.
 */
abstract class FailureHandledRunnable implements Runnable {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final MassIndexingNotifier notifier;

	protected FailureHandledRunnable(MassIndexingNotifier notifier) {
		this.notifier = notifier;
	}

	@Override
	public final void run() {
		boolean interrupted = false;
		try {
			runWithFailureHandler();
		}
		catch (MassIndexingOperationHandledFailureException e) {
			// This exception has already been reported; just clean up then propagate it.
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

			throw e;
		}
		catch (InterruptedException e) {
			interrupted = true;
			try {
				cleanUpOnInterruption();
			}
			catch (RuntimeException | InterruptedException e2) {
				e.addSuppressed( e2 );
			}

			// This may throw an exception, and we're fine with not catching it.
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

			// This may throw an exception, and we're fine with not catching it.
			notifyFailure( e );

			// Also propagate the exception
			throw new MassIndexingOperationHandledFailureException( e );
		}
		catch (Error e) {
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
			catch (Error e2) {
				e.addSuppressed( e2 );
			}

			try {
				notifyError( e );
			}
			// We always want to throw the original error, even of something was thrown in the try block.
			catch (RuntimeException | Error e2) {
				e.addSuppressed( e2 );
			}

			// Also propagate the error
			throw e;
		}
		finally {
			if ( interrupted ) {
				// Restore interruption signal
				Thread.currentThread().interrupt();
			}
		}

		if ( !interrupted ) {
			// This may throw an exception, and we're fine with not catching it.
			notifySuccess();
		}
	}

	protected abstract void runWithFailureHandler() throws InterruptedException;

	protected abstract void cleanUpOnInterruption() throws InterruptedException;

	protected abstract void cleanUpOnFailure() throws InterruptedException;

	protected final MassIndexingNotifier getNotifier() {
		return notifier;
	}

	protected void notifySuccess() {
		// Do nothing by default
	}

	protected void notifyError(Error error) {
		notifier.notifyError( error );
	}

	protected void notifyInterrupted(InterruptedException exception) {
		// By default, just report the interruption to the coordinator...
		notifier.notifyInterrupted( exception );
		/// ... and to the caller.
		throw new MassIndexingOperationHandledFailureException( exception );
		// run() will reset the interrupt flag on this thread, so we don't need to do it here.
	}

	protected void notifyFailure(RuntimeException exception) {
		notifier.notifyRunnableFailure( exception, operationName() );
	}

	protected String operationName() {
		return log.massIndexerOperation();
	}

}
