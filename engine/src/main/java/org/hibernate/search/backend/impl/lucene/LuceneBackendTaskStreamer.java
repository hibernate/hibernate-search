/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.impl.lucene;

import java.util.concurrent.locks.Lock;

import org.apache.lucene.index.IndexWriter;
import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.impl.lucene.works.LuceneWorkVisitor;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Version of LuceneBackendQueueTask meant for streaming operations only,
 * so single operations instead of queues and to reuse this same instance
 * multiple times.
 * Since this implementation is not async, the ErrorContextBuilder is not
 * applied; this should be wrapping the invoker code, as it does for example
 * in the MassIndexer.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2012 Red Hat Inc.
 */
final class LuceneBackendTaskStreamer {

	private static final Log log = LoggerFactory.make();

	private final LuceneWorkVisitor workVisitor;
	private final Lock modificationLock;
	private final AbstractWorkspaceImpl workspace;

	public LuceneBackendTaskStreamer(LuceneBackendResources resources) {
		this.workVisitor = resources.getVisitor();
		this.workspace = resources.getWorkspace();
		this.modificationLock = resources.getParallelModificationLock();
	}

	public void doWork(final LuceneWork work, final IndexingMonitor monitor) {
		modificationLock.lock();
		try {
			IndexWriter indexWriter = workspace.getIndexWriter();
			if ( indexWriter == null ) {
				log.cannotOpenIndexWriterCausePreviousError();
				return;
			}
			boolean errors = true;
			try {
				work.getWorkDelegate( workVisitor ).performWork( work, indexWriter, monitor );
				errors = false;
			}
			finally {
				workspace.afterTransactionApplied( errors, true );
			}
		}
		finally {
			modificationLock.unlock();
		}
	}

}
