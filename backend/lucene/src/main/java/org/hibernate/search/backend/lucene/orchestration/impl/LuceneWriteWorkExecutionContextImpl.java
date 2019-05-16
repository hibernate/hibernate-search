/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.orchestration.impl;

import java.io.IOException;

import org.apache.lucene.index.IndexWriter;

import org.hibernate.search.backend.lucene.lowlevel.writer.impl.IndexWriterHolder;
import org.hibernate.search.backend.lucene.work.impl.LuceneWriteWorkExecutionContext;

class LuceneWriteWorkExecutionContextImpl implements LuceneWriteWorkExecutionContext {

	private final IndexWriterHolder indexWriterHolder;

	LuceneWriteWorkExecutionContextImpl(IndexWriterHolder indexWriterHolder) {
		this.indexWriterHolder = indexWriterHolder;
	}

	@Override
	public IndexWriter getIndexWriter() throws IOException {
		return indexWriterHolder.getIndexWriter();
	}
}
