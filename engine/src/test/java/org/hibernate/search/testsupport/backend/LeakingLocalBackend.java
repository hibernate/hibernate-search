/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.testsupport.backend;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.impl.LocalBackendQueueProcessor;

/**
 * This backend wraps the default Lucene backend to leak out the last performed list of work for testing purposes: tests
 * can inspect the queue being sent to a backend.
 *
 * @author Sanne Grinovero
 *
 */
public class LeakingLocalBackend extends LocalBackendQueueProcessor {

	private static volatile List<LuceneWork> lastProcessedQueue = new ArrayList<LuceneWork>();

	@Override
	public void close() {
		lastProcessedQueue = new ArrayList<LuceneWork>();
		super.close();
	}

	public static List<LuceneWork> getLastProcessedQueue() {
		return lastProcessedQueue;
	}

	public static void reset() {
		lastProcessedQueue = new ArrayList<LuceneWork>();
	}

	@Override
	public void applyWork(List<LuceneWork> workList, IndexingMonitor monitor) {
		super.applyWork( workList, monitor );
		lastProcessedQueue = workList;
	}

}
