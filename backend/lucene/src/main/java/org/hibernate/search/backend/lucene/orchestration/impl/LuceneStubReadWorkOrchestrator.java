/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.orchestration.impl;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.lucene.index.spi.ReaderProvider;
import org.hibernate.search.backend.lucene.work.impl.LuceneReadWork;
import org.hibernate.search.util.common.impl.Futures;
import org.hibernate.search.util.common.impl.SuppressingCloser;


/**
 * @author Yoann Rodiere
 * @author Guillaume Smet
 */
public class LuceneStubReadWorkOrchestrator implements LuceneReadWorkOrchestrator {

	// Protected by synchronization on updates
	private CompletableFuture<?> latestFuture = CompletableFuture.completedFuture( null );

	public LuceneStubReadWorkOrchestrator() {
	}

	@Override
	public void close() {
		latestFuture.join();
	}

	@Override
	public synchronized <T> CompletableFuture<T> submit(Set<String> indexNames, Set<ReaderProvider> readerProviders,
			LuceneReadWork<T> work) {
		CompletableFuture<T> future = latestFuture.thenCompose( Futures.safeComposer(
				ignored -> {
					LuceneStubReadWorkExecutionContext context =
							new LuceneStubReadWorkExecutionContext( indexNames, readerProviders );
					Throwable throwable = null;
					try {
						// FIXME for now everything is blocking here, we need a non blocking wrapper on top of the IndexReader
						return new CompletableFuture<>( work.execute( context ) );
					}
					catch (Throwable t) {
						// Just remember something went wrong
						throwable = t;
						throw t;
					}
					finally {
						if ( throwable == null ) {
							context.close();
						}
						else {
							// Take care not to erase the main error if closing the context fails: use addSuppressed() instead
							new SuppressingCloser( throwable )
									.push( context );
						}
					}
				}
		) );
		// Ignore errors from this work in future works and during close(): error handling is the client's responsibility.
		latestFuture = future.exceptionally( ignore -> null );
		return future;
	}

}
