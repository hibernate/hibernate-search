/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.orchestration.impl;

import java.util.Set;

import org.hibernate.search.backend.lucene.index.spi.ReaderProvider;
import org.hibernate.search.backend.lucene.search.reader.impl.MultiReaderFactory;
import org.hibernate.search.backend.lucene.work.impl.LuceneReadWorkExecutionContext;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.reporting.EventContext;

import org.apache.lucene.index.IndexReader;

class LuceneStubReadWorkExecutionContext implements AutoCloseable, LuceneReadWorkExecutionContext {

	private final Set<String> indexNames;
	private final IndexReader indexReader;

	LuceneStubReadWorkExecutionContext(Set<String> indexNames, Set<ReaderProvider> readerProviders) {
		this.indexNames = indexNames;
		this.indexReader = MultiReaderFactory.openReader( indexNames, readerProviders );
	}

	@Override
	public void close() {
		MultiReaderFactory.closeReader( indexReader );
	}

	@Override
	public IndexReader getIndexReader() {
		return indexReader;
	}

	@Override
	public EventContext getEventContext() {
		return EventContexts.fromIndexNames( indexNames );
	}
}
