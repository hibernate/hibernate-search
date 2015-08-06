/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.impl.lucene.overrides;

import java.io.IOException;

import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.MergePolicy;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.ThreadInterruptedException;
import org.hibernate.search.exception.ErrorHandler;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * We customize Lucene's ConcurrentMergeScheduler to route eventual exceptions to our configurable error handler.
 *
 * @see ErrorHandler
 * @since 3.3
 * @author Sanne Grinovero
 */
//TODO think about using an Executor instead of starting Threads directly
public class ConcurrentMergeScheduler extends org.apache.lucene.index.ConcurrentMergeScheduler {

	private static final Log log = LoggerFactory.make();

	private final ErrorHandler errorHandler;
	private final String indexName;

	public ConcurrentMergeScheduler(ErrorHandler errorHandler, String indexName) {
		this.errorHandler = errorHandler;
		this.indexName = indexName;
	}

	@Override
	protected void handleMergeException(Directory dir, Throwable t) {
		try {
			super.handleMergeException( dir, t );
		}
		catch (ThreadInterruptedException ie) {
			Thread.currentThread().interrupt();
		}
		catch (Exception ex) {
			errorHandler.handleException( log.exceptionDuringIndexMergeOperation() , ex );
		}
	}

	/*
	 * Overrides method to customize the thread Name
	 * @see org.apache.lucene.index.ConcurrentMergeScheduler#getMergeThread(org.apache.lucene.index.IndexWriter, org.apache.lucene.index.MergePolicy.OneMerge)
	 */
	@Override
	protected synchronized MergeThread getMergeThread(IndexWriter writer, MergePolicy.OneMerge merge) throws IOException {
		final MergeThread thread = new MergeThread( writer, merge );
		thread.setDaemon( true );
		thread.setName( "Lucene Merge Thread #" + mergeThreadCount++ + " for index " + indexName );
		return thread;
	}

}
