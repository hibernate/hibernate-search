/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2014 Red Hat Inc.
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
