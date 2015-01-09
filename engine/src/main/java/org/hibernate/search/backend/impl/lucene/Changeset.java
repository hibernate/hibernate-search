/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.impl.lucene;

import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.backend.LuceneWork;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.LockSupport;

/**
 * A Changeset is a work to be applied to the index and its associated producer
 *
 * @author gustavonalle
 */
public final class Changeset {

	private final List<LuceneWork> workList;
	private final Thread producer;
	private final IndexingMonitor monitor;
	private volatile boolean processed = false;

	public Changeset(List<LuceneWork> workList, Thread producer, IndexingMonitor monitor) {
		this.workList = workList;
		this.producer = producer;
		this.monitor = monitor;
	}

	Iterator<LuceneWork> getWorkListIterator() {
		return workList.iterator();
	}

	IndexingMonitor getMonitor() {
		return monitor;
	}

	boolean isProcessed() {
		return processed;
	}

	public void markProcessed() {
		processed = true;
		LockSupport.unpark( producer );
	}

}
