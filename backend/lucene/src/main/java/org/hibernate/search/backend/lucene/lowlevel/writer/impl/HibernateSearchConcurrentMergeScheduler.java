/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.writer.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.engine.environment.thread.spi.ThreadProvider;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.engine.reporting.IndexFailureContext;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.apache.lucene.index.ConcurrentMergeScheduler;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.MergePolicy;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.ThreadInterruptedException;

/**
 * We customize Lucene's ConcurrentMergeScheduler to route eventual exceptions to our configurable failure handler
 * and override the name of merge threads.
 *
 * @see FailureHandler
 * @since 3.3
 * @author Sanne Grinovero
 */
//TODO think about using an Executor instead of starting Threads directly
class HibernateSearchConcurrentMergeScheduler extends ConcurrentMergeScheduler {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final String indexName;
	private final String contextDescription;
	private final ThreadProvider threadProvider;
	private final FailureHandler failureHandler;

	HibernateSearchConcurrentMergeScheduler(String indexName, String contextDescription,
			ThreadProvider threadProvider,
			FailureHandler failureHandler) {
		this.indexName = indexName;
		this.contextDescription = contextDescription;
		this.threadProvider = threadProvider;
		this.failureHandler = failureHandler;
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
			IndexFailureContext.Builder contextBuilder = IndexFailureContext.builder();
			contextBuilder.indexName( indexName );
			contextBuilder.throwable( ex );
			contextBuilder.failingOperation( log.indexMergeOperation() );
			failureHandler.handle( contextBuilder.build() );
		}
	}

	@Override
	protected synchronized MergeThread getMergeThread(IndexWriter writer, MergePolicy.OneMerge merge) {
		final MergeThread thread = new MergeThread( writer, merge );
		thread.setDaemon( true );
		thread.setName(
				threadProvider.createThreadName(
						"Lucene Merge Thread for " + contextDescription,
						mergeThreadCount++
				)
		);
		return thread;
	}

}
