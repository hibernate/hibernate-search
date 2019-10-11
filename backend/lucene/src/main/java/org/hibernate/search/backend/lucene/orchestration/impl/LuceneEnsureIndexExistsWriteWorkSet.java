/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.orchestration.impl;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.reporting.spi.IndexFailureContextImpl;

/**
 * A special workset that won't trigger the creation of the index writer.
 * <p>
 * Useful to make sure that read-only applications never create any index writer.
 */
class LuceneEnsureIndexExistsWriteWorkSet implements LuceneWriteWorkSet {

	private final CompletableFuture<?> future;

	LuceneEnsureIndexExistsWriteWorkSet(CompletableFuture<?> future) {
		this.future = future;
	}

	@Override
	public void submitTo(LuceneWriteWorkProcessor processor) {
		try {
			processor.ensureIndexExists();
			future.complete( null );
		}
		catch (RuntimeException e) {
			markAsFailed( e );
			// FIXME HSEARCH-3735 This is temporary and should be removed when all failures are reported to the mapper directly
			IndexFailureContextImpl.Builder failureContextBuilder = new IndexFailureContextImpl.Builder();
			failureContextBuilder.throwable( e );
			failureContextBuilder.failingOperation( "Index initialization" );
			processor.getFailureHandler().handle( failureContextBuilder.build() );
		}
	}

	@Override
	public void markAsFailed(Throwable t) {
		future.completeExceptionally( t );
	}
}
