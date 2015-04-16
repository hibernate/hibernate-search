/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.impl.lucene.works;

import org.apache.lucene.index.IndexWriter;
import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.backend.LuceneWork;

/**
 * <p>LuceneWorkDelegate interface.</p>
 *
 * @author Sanne Grinovero
 */
public interface LuceneWorkExecutor {

	/**
	 * Will perform work on an IndexWriter.
	 *
	 * @param work the LuceneWork to apply to the IndexWriter.
	 * @param writer the IndexWriter to use.
	 * @param monitor will be notified of performed operations
	 * @throws java.lang.UnsupportedOperationException when the work is not compatible with an IndexWriter.
	 */
	void performWork(LuceneWork work, IndexWriter writer, IndexingMonitor monitor);

}
