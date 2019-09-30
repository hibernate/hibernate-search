/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.index.impl;

import java.io.Closeable;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.lowlevel.index.impl.IndexAccessor;
import org.hibernate.search.backend.lucene.lowlevel.reader.spi.IndexReaderHolder;
import org.hibernate.search.backend.lucene.orchestration.impl.LuceneWriteWorkOrchestrator;
import org.hibernate.search.backend.lucene.orchestration.impl.LuceneWriteWorkOrchestratorImplementor;
import org.hibernate.search.backend.lucene.work.impl.LuceneWorkFactory;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.impl.SuppressingCloser;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public final class Shard implements Closeable {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final LuceneWorkFactory workFactory;
	private final IndexAccessor indexAccessor;
	private final LuceneWriteWorkOrchestratorImplementor writeOrchestrator;

	Shard(LuceneWorkFactory workFactory, IndexAccessor indexAccessor,
			LuceneWriteWorkOrchestratorImplementor writeOrchestrator) {
		this.workFactory = workFactory;
		this.indexAccessor = indexAccessor;
		this.writeOrchestrator = writeOrchestrator;
	}

	public CompletableFuture<?> start() {
		try {
			indexAccessor.start();
			writeOrchestrator.start();
			return writeOrchestrator.submit(
					workFactory.ensureIndexExists(),
					DocumentCommitStrategy.NONE,
					DocumentRefreshStrategy.NONE
			);
		}
		catch (IOException | RuntimeException e) {
			new SuppressingCloser( e )
					.push( indexAccessor )
					.push( writeOrchestrator );
			throw log.unableToInitializeIndexDirectory(
					e.getMessage(),
					indexAccessor.getIndexEventContext(),
					e
			);
		}
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
