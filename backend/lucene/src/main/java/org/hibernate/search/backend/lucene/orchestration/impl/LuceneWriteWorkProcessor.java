/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.orchestration.impl;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.lowlevel.writer.impl.IndexWriterDelegator;
import org.hibernate.search.backend.lucene.work.impl.LuceneWriteWork;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.backend.orchestration.spi.BatchingExecutor;
import org.hibernate.search.engine.common.spi.ContextualErrorHandler;
import org.hibernate.search.engine.common.spi.ErrorHandler;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reporting.EventContext;

/**
 * A thread-unsafe component responsible for applying write works to an index writer.
 * <p>
 * Ported from Search 5's LuceneBackendQueueTask, in particular.
 */
public class LuceneWriteWorkProcessor implements BatchingExecutor.WorkProcessor {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final EventContext indexEventContext;
	private final IndexWriterDelegator indexWriterDelegator;
	private final LuceneWriteWorkExecutionContextImpl context;
	private final ErrorHandler errorHandler;

	private boolean hasUncommittedWorks;

	private Throwable workSetFailure;
	private ContextualErrorHandler workSetContextualErrorHandler;
	private boolean workSetForcesCommit;

	public LuceneWriteWorkProcessor(EventContext indexEventContext, IndexWriterDelegator indexWriterDelegator,
			ErrorHandler errorHandler) {
		this.indexEventContext = indexEventContext;
		this.indexWriterDelegator = indexWriterDelegator;
		this.context = new LuceneWriteWorkExecutionContextImpl( indexEventContext, indexWriterDelegator );
		this.errorHandler = errorHandler;
	}

	@Override
	public void beginBatch() {
		// Nothing to do
	}

	@Override
	public CompletableFuture<?> endBatch() {
		try {
			commitIfNecessary();
		}
		catch (RuntimeException e) {
			try {
				cleanUpAfterError();
			}
			catch (RuntimeException e2) {
				e.addSuppressed( e2 );
			}
			errorHandler.handleException( e.getMessage(), e );
		}
		// Everything was already executed, so just return a completed future.
		return CompletableFuture.completedFuture( null );
	}

	void beforeWorkSet(DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy) {
		workSetFailure = null;
		workSetContextualErrorHandler = null;
		workSetForcesCommit = DocumentCommitStrategy.FORCE.equals( commitStrategy )
				// We need to commit in order to make the changes visible
				// TODO HSEARCH-3117 this may not be true with the NRT implementation from Search 5
				|| DocumentRefreshStrategy.FORCE.equals( refreshStrategy );
	}

	/**
	 * This bypasses the normal {@link #submit(LuceneWriteWork)} method in order
	 * to avoid setting {@link #hasUncommittedWorks} to {@code true},
	 * so that we skip the end-of-batch commit and thus avoid the creation of an IndexWriter,
	 * which would be pointless in this case.
	 */
	void ensureIndexExists() {
		try {
			indexWriterDelegator.ensureIndexExists();
		}
		catch (IOException | RuntimeException e) {
			workSetFailure = log.unableToInitializeIndexDirectory(
					e.getMessage(), indexEventContext, e
			);
			// TODO HSEARCH-1375 report the index name?
			//workSetContextualErrorHandler.indexManager( resources.getIndexManager() );
			getWorkSetContextualErrorHandler().markAsFailed( "Index initialization", e );
		}
	}

	<T> T submit(LuceneWriteWork<T> work) {
		if ( workSetFailure == null ) {
			try {
				hasUncommittedWorks = true;
				return work.execute( context );
			}
			catch (RuntimeException e) {
				workSetFailure = e;
				// TODO HSEARCH-1375 report the index name?
				//workSetContextualErrorHandler.indexManager( resources.getIndexManager() );
				getWorkSetContextualErrorHandler().markAsFailed( work.getInfo(), e );
				return null;
			}
		}
		else {
			getWorkSetContextualErrorHandler().markAsSkipped( work.getInfo() );
			return null;
		}
	}

	<T> void afterWorkSet(CompletableFuture<T> future, T resultIfSuccess) {
		if ( workSetFailure == null && workSetForcesCommit ) {
			try {
				commitIfNecessary();
			}
			catch (RuntimeException e) {
				workSetFailure = e;
				getWorkSetContextualErrorHandler().addThrowable( e );
			}
		}
		if ( workSetFailure != null ) {
			try {
				cleanUpAfterError();
			}
			catch (RuntimeException e) {
				workSetFailure.addSuppressed( e );
			}
			future.completeExceptionally( workSetFailure );
			getWorkSetContextualErrorHandler().handle();
		}
		else {
			future.complete( resultIfSuccess );
		}
	}

	private void commitIfNecessary() {
		if ( hasUncommittedWorks ) {
			try {
				// TODO HSEARCH-3117 restore the commit policy feature to allow scheduled commits?
				hasUncommittedWorks = false;
				indexWriterDelegator.commit();
			}
			catch (RuntimeException | IOException e) {
				throw log.unableToCommitIndex( indexEventContext, e );
			}
		}
	}

	private void cleanUpAfterError() {
		try {
			hasUncommittedWorks = false;
			/*
			 * Note this will close the index writer,
			 * which with the default settings will trigger a commit.
			 */
			indexWriterDelegator.forceLockRelease();
		}
		catch (RuntimeException | IOException e) {
			throw log.unableToCleanUpAfterError( indexEventContext, e );
		}
	}

	private ContextualErrorHandler getWorkSetContextualErrorHandler() {
		if ( workSetContextualErrorHandler == null ) {
			workSetContextualErrorHandler = errorHandler.createContextualHandler();
		}
		return workSetContextualErrorHandler;
	}
}
