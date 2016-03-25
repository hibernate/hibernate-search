/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.impl.lucene;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.Lock;

import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.exception.impl.ErrorContextBuilder;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Apply the operations to Lucene directories.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 * @author John Griffin
 * @author Sanne Grinovero
 */
final class LuceneBackendQueueTask implements Runnable {

	private static final Log log = LoggerFactory.make();

	private final Lock modificationLock;
	private final LuceneBackendResources resources;
	private final Iterable<LuceneWork> workList;
	private final IndexingMonitor monitor;

	LuceneBackendQueueTask(Iterable<LuceneWork> workList, LuceneBackendResources resources, IndexingMonitor monitor) {
		this.workList = workList;
		this.resources = resources;
		this.monitor = monitor;
		this.modificationLock = resources.getParallelModificationLock();
	}

	@Override
	public void run() {
		modificationLock.lock();
		try {
			applyUpdates();
		}
		catch (InterruptedException e) {
			log.interruptedWhileWaitingForIndexActivity( e );
			Thread.currentThread().interrupt();
			handleException( e );
		}
		catch (Exception e) {
			log.backendError( e );
			handleException( e );
		}
		finally {
			modificationLock.unlock();
		}
	}

	private void handleException(Exception e) {
		ErrorContextBuilder builder = new ErrorContextBuilder();
		builder.allWorkToBeDone( workList );
		builder.errorThatOccurred( e );
		resources.getErrorHandler().handle( builder.createErrorContext() );
	}

	/**
	 * Applies all modifications to the index in parallel using the workers executor
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	private void applyUpdates() throws InterruptedException, ExecutionException {
		AbstractWorkspaceImpl workspace = resources.getWorkspace();

		ErrorContextBuilder errorContextBuilder = new ErrorContextBuilder();
		errorContextBuilder.allWorkToBeDone( workList );

		IndexWriterDelegate delegate = workspace.getIndexWriterDelegate( errorContextBuilder );
		if ( delegate == null ) {
			log.cannotOpenIndexWriterCausePreviousError();
			return;
		}

		boolean someFailureHappened = true;
		LuceneWork currentOperation = null; // to nicely report errors
		try {
			for ( LuceneWork luceneWork : workList ) {
				currentOperation = luceneWork;
				performWork( luceneWork, resources, delegate, monitor );
				errorContextBuilder.workCompleted( currentOperation );
			}
			currentOperation = null;
			workspace.optimizerPhase();
			someFailureHappened = false;
		}
		catch (RuntimeException re) {
			errorContextBuilder.errorThatOccurred( re );
			if ( currentOperation != null ) {
				errorContextBuilder.addWorkThatFailed( currentOperation );
			}
			resources.getErrorHandler().handle( errorContextBuilder.createErrorContext() );
		}
		finally {
			workspace.afterTransactionApplied( someFailureHappened, false );
		}
	}

	static void performWork(final LuceneWork work, final LuceneBackendResources resources, final IndexWriterDelegate delegate, final IndexingMonitor monitor) {
		work.acceptIndexWorkVisitor( resources.getWorkVisitor(), null ).performWork( work, delegate, monitor );
	}

}
