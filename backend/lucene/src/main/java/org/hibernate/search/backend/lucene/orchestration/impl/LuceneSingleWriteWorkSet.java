/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.orchestration.impl;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.lucene.work.impl.LuceneWriteWork;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.reporting.IndexFailureContext;
import org.hibernate.search.engine.reporting.spi.IndexFailureContextImpl;

class LuceneSingleWriteWorkSet<T> implements LuceneWriteWorkSet {
	private final LuceneWriteWork<T> work;
	private final CompletableFuture<T> future;
	private final DocumentCommitStrategy commitStrategy;
	private final DocumentRefreshStrategy refreshStrategy;

	LuceneSingleWriteWorkSet(LuceneWriteWork<T> work, CompletableFuture<T> future,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy) {
		this.work = work;
		this.future = future;
		this.commitStrategy = commitStrategy;
		this.refreshStrategy = refreshStrategy;
	}

	@Override
	public void submitTo(LuceneWriteWorkProcessor processor) {
		processor.beforeWorkSet( commitStrategy, refreshStrategy );
		try {
			T result = processor.submit( work );
			processor.afterSuccessfulWorkSet();
			future.complete( result );
		}
		catch (RuntimeException e) {
			markAsFailed( e );
			// FIXME HSEARCH-3735 This is temporary and should be removed when all failures are reported to the mapper directly
			IndexFailureContextImpl.Builder failureContextBuilder = new IndexFailureContextImpl.Builder();
			failureContextBuilder.throwable( e );
			failureContextBuilder.failingOperation( work.getInfo() );
			IndexFailureContext failureContext = failureContextBuilder.build();
			processor.getFailureHandler().handle( failureContext );
		}
	}

	@Override
	public void markAsFailed(Throwable t) {
		future.completeExceptionally( t );
	}
}
