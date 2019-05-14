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
import org.hibernate.search.backend.lucene.work.impl.LuceneWriteWork;
import org.hibernate.search.engine.backend.orchestration.spi.BatchingExecutor;
import org.hibernate.search.engine.common.spi.ContextualErrorHandler;
import org.hibernate.search.engine.common.spi.ErrorHandler;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reporting.EventContext;

import org.apache.lucene.index.IndexWriter;

/**
 * A thread-unsafe component responsible for applying write works to an index writer.
 * <p>
 * Ported from Search 5's LuceneBackendQueueTask, in particular.
 */
public class LuceneWriteWorkProcessor implements BatchingExecutor.Processor {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final EventContext indexEventContext;
	private final LuceneWriteWorkExecutionContextImpl context;
	private final ErrorHandler errorHandler;

	private Throwable workSetFailure;
	private ContextualErrorHandler workSetContextualErrorHandler;

	public LuceneWriteWorkProcessor(EventContext indexEventContext, IndexWriter indexWriter, ErrorHandler errorHandler) {
		this.indexEventContext = indexEventContext;
		this.context = new LuceneWriteWorkExecutionContextImpl( indexWriter );
		this.errorHandler = errorHandler;
	}

	@Override
	public void beginBatch() {
		// Nothing to do
	}

	@Override
	public CompletableFuture<?> endBatch() {
		// FIXME move the commit here when works do not require immediate commit? (e.g. mass indexing)
		// Everything was already executed, so just return a completed future.
		return CompletableFuture.completedFuture( null );
	}

	void beforeWorkSet() {
		workSetFailure = null;
		workSetContextualErrorHandler = null;
	}

	<T> T submit(LuceneWriteWork<T> work) {
		if ( workSetFailure == null ) {
			try {
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
		IndexWriter indexWriter = context.getIndexWriter();
		if ( workSetFailure == null ) {
			try {
				doCommit( indexWriter );
			}
			catch (RuntimeException e) {
				workSetFailure = e;
				getWorkSetContextualErrorHandler().addThrowable( e );
			}
		}
		if ( workSetFailure != null ) {
			future.completeExceptionally( workSetFailure );
			// FIXME close the index writer in case of error?
			//  this will require an IndexWriterHolder
			//  see org.hibernate.search.backend.impl.lucene.IndexWriterHolder.forceLockRelease
			getWorkSetContextualErrorHandler().handle();
		}
		else {
			future.complete( resultIfSuccess );
		}
	}

	private void doCommit(IndexWriter indexWriter) {
		try {
			// TODO HSEARCH-3117 restore the commit policy feature to allow scheduled commits?
			indexWriter.commit();
		}
		catch (RuntimeException | IOException e) {
			throw log.unableToCommitIndex( indexEventContext, e );
		}

	}

	private ContextualErrorHandler getWorkSetContextualErrorHandler() {
		if ( workSetContextualErrorHandler == null ) {
			workSetContextualErrorHandler = errorHandler.createContextualHandler();
		}
		return workSetContextualErrorHandler;
	}
}
