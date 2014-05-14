/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.impl.lucene;

import org.apache.lucene.index.IndexWriter;
import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.backend.LuceneWork;

/**
 * Applies an update operation to the IndexWriter
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public final class SingleTaskRunnable implements Runnable {

	private final LuceneWork work;
	private final LuceneBackendResources resources;
	private final IndexWriter indexWriter;
	private final IndexingMonitor monitor;

	public SingleTaskRunnable(LuceneWork work, LuceneBackendResources resources, IndexWriter indexWriter, IndexingMonitor monitor) {
		this.work = work;
		this.resources = resources;
		this.indexWriter = indexWriter;
		this.monitor = monitor;
	}

	@Override
	public void run() {
		performWork( work, resources, indexWriter, monitor );
	}

	static void performWork(final LuceneWork work, final LuceneBackendResources resources, final IndexWriter indexWriter, final IndexingMonitor monitor) {
		work.getWorkDelegate( resources.getVisitor() ).performWork( work, indexWriter, monitor );
	}

}
