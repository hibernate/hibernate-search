/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.orchestration.impl;

import org.apache.lucene.index.IndexWriter;
import org.hibernate.search.backend.lucene.work.impl.LuceneWriteWorkExecutionContext;

/**
 * @author Guillaume Smet
 */
public class LuceneStubWriteWorkExecutionContext implements LuceneWriteWorkExecutionContext {

	private final IndexWriter indexWriter;

	public LuceneStubWriteWorkExecutionContext(IndexWriter indexWriter) {
		this.indexWriter = indexWriter;
	}

	@Override
	public IndexWriter getIndexWriter() {
		return indexWriter;
	}
}
