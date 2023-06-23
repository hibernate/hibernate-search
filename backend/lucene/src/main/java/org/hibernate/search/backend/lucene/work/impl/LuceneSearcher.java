/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.work.impl;

import java.io.IOException;

import org.hibernate.search.backend.lucene.lowlevel.reader.impl.IndexReaderMetadataResolver;
import org.hibernate.search.engine.search.timeout.spi.TimeoutManager;

import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;

public interface LuceneSearcher<R, ER> {

	R search(IndexSearcher indexSearcher, IndexReaderMetadataResolver metadataResolver,
			int offset, Integer limit, int totalHitCountThreshold)
			throws IOException;

	ER scroll(IndexSearcher indexSearcher, IndexReaderMetadataResolver metadataResolver,
			int offset, int limit, int totalHitCountThreshold)
			throws IOException;

	int count(IndexSearcher indexSearcher) throws IOException;

	Explanation explain(IndexSearcher indexSearcher, int luceneDocId) throws IOException;

	Query getLuceneQueryForExceptions();

	void setTimeoutManager(TimeoutManager timeoutManager);
}
