/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.massindexing.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.massindexing.MassIndexingEnvironment;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * Common parent of all Runnable implementations for the batch indexing:
 * share the code for handling runtime exceptions.
 */
public abstract class PojoMassIndexingFailureHandledRunnable implements Runnable {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final PojoMassIndexingNotifier notifier;
	private final MassIndexingEnvironment environment;

	protected PojoMassIndexingFailureHandledRunnable(PojoMassIndexingNotifier notifier, MassIndexingEnvironment environment) {
		this.notifier = notifier;
		this.environment = environment;
	}

	@Override
	public final void run() {
		boolean interrupted = false;

		try {
			beforeExecution();
			try {
				runWithFailureHandler();
			}
			finally {
				// will only make an attempt to call `afterExecution()` if `beforeExecution()` call was successful.
				afterExecution();
			}
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
			catch (RuntimeException | Error e2) {
				e.addSuppressed( e2 );
			}
			catch (InterruptedException e2) {
				interrupted = true;
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

	protected MassIndexingEnvironment.Context createMassIndexingEnvironmentContext() {
		throw new UnsupportedOperationException( "There's no context supported for " + this.getClass().getSimpleName() );
	}

	protected boolean supportsThreadLifecycleHooks() {
		return false;
	}

	protected void beforeExecution() {
		if ( supportsThreadLifecycleHooks() ) {
			getMassIndexingEnvironment().beforeExecution(
					createMassIndexingEnvironmentContext()
			);
		}
	}

	protected void afterExecution() {
		if ( supportsThreadLifecycleHooks() ) {
			getMassIndexingEnvironment().afterExecution(
					createMassIndexingEnvironmentContext()
			);
		}
	}

	protected final PojoMassIndexingNotifier getNotifier() {
		return notifier;
	}

	protected final MassIndexingEnvironment getMassIndexingEnvironment() {
		return environment;
	}

	protected void notifySuccess() {
		// Do nothing by default
	}

	protected void notifyError(Error error) {
		notifier.reportError( error );
	}

	protected void notifyInterrupted(InterruptedException exception) {
		// By default, just report the interruption to the coordinator...
		notifier.reportInterrupted( exception );
		/// ... and to the caller.
		throw new MassIndexingOperationHandledFailureException( exception );
		// run() will reset the interrupt flag on this thread, so we don't need to do it here.
	}

	protected void notifyFailure(RuntimeException exception) {
		notifier.reportRunnableFailure( exception, operationName() );
	}

	protected String operationName() {
		return log.massIndexerOperation();
	}

}
