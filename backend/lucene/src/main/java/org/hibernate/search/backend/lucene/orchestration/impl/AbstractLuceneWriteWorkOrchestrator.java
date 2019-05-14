/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.orchestration.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.lucene.work.impl.LuceneWriteWork;
import org.hibernate.search.engine.backend.orchestration.spi.AbstractWorkOrchestrator;
import org.hibernate.search.engine.backend.orchestration.spi.BatchingExecutor;

/**
 * An abstract base for {@link LuceneWriteWorkOrchestratorImplementor} implementations.
 */
abstract class AbstractLuceneWriteWorkOrchestrator
		extends AbstractWorkOrchestrator<AbstractLuceneWriteWorkOrchestrator.LuceneWorkSet>
		implements LuceneWriteWorkOrchestratorImplementor {

	AbstractLuceneWriteWorkOrchestrator(String name) {
		super( name );
	}

	@Override
	public CompletableFuture<?> submit(List<LuceneWriteWork<?>> works) {
		CompletableFuture<Object> future = new CompletableFuture<>();
		submit( new LuceneMultipleWorkSet( works, future ) );
		return future;
	}

	@Override
	public <T> CompletableFuture<T> submit(LuceneWriteWork<T> work) {
		CompletableFuture<T> future = new CompletableFuture<>();
		submit( new LuceneSingleWorkSet<>( work, future ) );
		return future;
	}

	interface LuceneWorkSet extends BatchingExecutor.Task<LuceneWriteWorkProcessor> {
	}

	static class LuceneMultipleWorkSet implements LuceneWorkSet {
		private final List<LuceneWriteWork<?>> works;
		private final CompletableFuture<Object> future;

		LuceneMultipleWorkSet(List<LuceneWriteWork<?>> works, CompletableFuture<Object> future) {
			this.works = new ArrayList<>( works );
			this.future = future;
		}

		@Override
		public void submitTo(LuceneWriteWorkProcessor processor) {
			processor.beforeWorkSet();
			for ( LuceneWriteWork<?> work : works ) {
				processor.submit( work );
			}
			processor.afterWorkSet( future, null );
		}

		@Override
		public void markAsFailed(Throwable t) {
			future.completeExceptionally( t );
		}
	}

	static class LuceneSingleWorkSet<T> implements LuceneWorkSet {
		private final LuceneWriteWork<T> work;
		private final CompletableFuture<T> future;

		LuceneSingleWorkSet(LuceneWriteWork<T> work, CompletableFuture<T> future) {
			this.work = work;
			this.future = future;
		}

		@Override
		public void submitTo(LuceneWriteWorkProcessor processor) {
			processor.beforeWorkSet();
			T result = processor.submit( work );
			processor.afterWorkSet( future, result );
		}

		@Override
		public void markAsFailed(Throwable t) {
			future.completeExceptionally( t );
		}
	}

}
