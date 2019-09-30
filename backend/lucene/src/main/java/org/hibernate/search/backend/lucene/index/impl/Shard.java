/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.index.impl;

import java.io.Closeable;
import java.io.IOException;

import org.hibernate.search.backend.lucene.lowlevel.index.impl.IndexAccessor;
import org.hibernate.search.backend.lucene.lowlevel.reader.spi.IndexReaderHolder;
import org.hibernate.search.backend.lucene.orchestration.impl.LuceneWriteWorkOrchestrator;
import org.hibernate.search.backend.lucene.orchestration.impl.LuceneWriteWorkOrchestratorImplementor;
import org.hibernate.search.util.common.impl.Closer;

public final class Shard implements Closeable {

	private final IndexAccessor indexAccessor;
	private final LuceneWriteWorkOrchestratorImplementor writeOrchestrator;

	Shard(IndexAccessor indexAccessor, LuceneWriteWorkOrchestratorImplementor writeOrchestrator) {
		this.indexAccessor = indexAccessor;
		this.writeOrchestrator = writeOrchestrator;
	}

	public void start() {
		indexAccessor.start();
		writeOrchestrator.start();
	}

	@Override
	public void close() throws IOException {
		try ( Closer<IOException> closer = new Closer<>() ) {
			closer.push( LuceneWriteWorkOrchestratorImplementor::close, writeOrchestrator );
			// Close the index writer after the orchestrators, when we're sure all works have been performed
			closer.push( IndexAccessor::close, indexAccessor );
		}
	}

	IndexReaderHolder openReader() throws IOException {
		return IndexReaderHolder.of( indexAccessor.openDirectoryIndexReader() );
	}

	LuceneWriteWorkOrchestrator getWriteOrchestrator() {
		return writeOrchestrator;
	}

	public IndexAccessor getIndexAccessorForTests() {
		return indexAccessor;
	}
}
