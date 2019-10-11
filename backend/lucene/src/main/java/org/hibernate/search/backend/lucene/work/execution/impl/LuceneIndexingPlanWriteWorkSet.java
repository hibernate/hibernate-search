/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.work.execution.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.lucene.orchestration.impl.LuceneWriteWorkProcessor;
import org.hibernate.search.backend.lucene.orchestration.impl.LuceneWriteWorkSet;
import org.hibernate.search.backend.lucene.work.impl.LuceneWriteWork;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.reporting.spi.IndexFailureContextImpl;

class LuceneIndexingPlanWriteWorkSet implements LuceneWriteWorkSet {
	private final List<LuceneWriteWork<?>> works;
	private final CompletableFuture<Object> future;
	private final DocumentCommitStrategy commitStrategy;
	private final DocumentRefreshStrategy refreshStrategy;

	LuceneIndexingPlanWriteWorkSet(List<LuceneWriteWork<?>> works, CompletableFuture<Object> future,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy) {
		this.works = new ArrayList<>( works );
		this.future = future;
		this.commitStrategy = commitStrategy;
		this.refreshStrategy = refreshStrategy;
	}

	@Override
	public void submitTo(LuceneWriteWorkProcessor processor) {
		processor.beforeWorkSet( commitStrategy, refreshStrategy );

		Throwable throwable = null;
		Object failingOperation = null;

		for ( LuceneWriteWork<?> work : works ) {
			try {
				processor.submit( work );
			}
			catch (RuntimeException e) {
				throwable = e;
				failingOperation = work.getInfo();
				break; // Don't even try to submit the next works
			}
		}

		if ( throwable == null ) {
			try {
				processor.afterSuccessfulWorkSet();
			}
			catch (RuntimeException e) {
				throwable = e;
				failingOperation = "Commit after a set of index works";
			}
		}

		if ( throwable == null ) {
			future.complete( null );
		}
		else {
			markAsFailed( throwable );
			// FIXME HSEARCH-3735 This is temporary and should be removed when all failures are reported to the mapper directly
			IndexFailureContextImpl.Builder failureContextBuilder = new IndexFailureContextImpl.Builder();
			failureContextBuilder.throwable( throwable );
			failureContextBuilder.failingOperation( failingOperation );
			// Even if some works succeeded, there's no guarantee they were actually committed to the index.
			// Report all works as uncommitted.
			for ( LuceneWriteWork<?> work : works ) {
				failureContextBuilder.uncommittedOperation( work.getInfo() );
			}
			processor.getFailureHandler().handle( failureContextBuilder.build() );
		}
	}

	@Override
	public void markAsFailed(Throwable t) {
		future.completeExceptionally( t );
	}
}
