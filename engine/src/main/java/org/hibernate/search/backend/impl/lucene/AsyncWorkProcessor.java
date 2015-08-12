/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.impl.lucene;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;


/**
 * This is the asynchronous backend logic for the LuceneBackendQueueProcessor.
 * It merely forwards batches of indexing work to the async Executor for this indexing backend.
 *
 * @author Sanne Grinovero (C) 2014 Red Hat Inc.
 * @since 5.0
 */
final class AsyncWorkProcessor implements WorkProcessor {

	private static final Log log = LoggerFactory.make();

	private volatile LuceneBackendResources resources;

	public AsyncWorkProcessor(LuceneBackendResources resources) {
		this.resources = resources;
	}

	@Override
	public void shutdown() {
		//no-op
	}

	@Override
	public void submit(List<LuceneWork> workList, IndexingMonitor monitor) {
		if ( workList.isEmpty() ) {
			// only log this error at trace level until we properly fix HSEARCH-1769
			if ( log.isTraceEnabled() ) {
				StringWriter stackTraceStringWriter = new StringWriter();
				PrintWriter stackTracePrintWriter = new PrintWriter( stackTraceStringWriter );
				new Throwable().printStackTrace( stackTracePrintWriter );
				log.workListShouldNeverBeEmpty( stackTraceStringWriter.toString() );
			}
			// skip that work
			return;
		}
		LuceneBackendQueueTask luceneBackendQueueProcessor = new LuceneBackendQueueTask(
				workList,
				resources,
				monitor
		);
		resources.getAsynchIndexingExecutor().execute( luceneBackendQueueProcessor );
	}

	@Override
	public void updateResources(LuceneBackendResources resources) {
		this.resources = resources;
	}

}
