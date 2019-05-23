/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.orchestration.impl;

import java.io.IOException;

import org.hibernate.search.backend.lucene.lowlevel.writer.impl.IndexWriterDelegator;
import org.hibernate.search.backend.lucene.work.impl.LuceneWriteWorkExecutionContext;

class LuceneWriteWorkExecutionContextImpl implements LuceneWriteWorkExecutionContext {

	private final IndexWriterDelegator indexWriterDelegator;

	LuceneWriteWorkExecutionContextImpl(IndexWriterDelegator indexWriterDelegator) {
		this.indexWriterDelegator = indexWriterDelegator;
	}

	@Override
	public IndexWriterDelegator getIndexWriterDelegator() throws IOException {
		return indexWriterDelegator;
	}
}
