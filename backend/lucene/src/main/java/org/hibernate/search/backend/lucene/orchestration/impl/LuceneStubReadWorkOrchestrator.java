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
import org.hibernate.search.util.common.impl.Throwables;


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
					LuceneStubQueryWorkExecutionContext context =
							new LuceneStubQueryWorkExecutionContext( indexNames, readerProviders );
					try {
						CompletableFuture<T> workFuture = work.execute( context );
						// Always close the execution context after the work is executed, regardless of errors
						return workFuture.handle( Futures.handler( (result, throwable) -> {
							if ( result != null ) {
								context.close();
								return result;
							}
							else {
								new SuppressingCloser( throwable )
										.push( context );
								throw Throwables.expectRuntimeException( throwable );
							}
						} ) );
					}
					catch (Throwable t) {
						new SuppressingCloser( t ).push( context );
						throw t;
					}
				}
		) );
		// Ignore errors from this work in future works and during close(): error handling is the client's responsibility.
		latestFuture = future.exceptionally( ignore -> null );
		return future;
	}

}
