/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
package org.hibernate.search.backend.impl.lucene.overrides;

import java.io.IOException;

import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.MergePolicy;
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
	protected void handleMergeException(Throwable t) {
		try {
			super.handleMergeException( t );
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
		thread.setThreadPriority( getMergeThreadPriority() );
		thread.setDaemon( true );
		thread.setName( "Lucene Merge Thread #" + mergeThreadCount++ + " for index " + indexName );
		return thread;
	}

}
