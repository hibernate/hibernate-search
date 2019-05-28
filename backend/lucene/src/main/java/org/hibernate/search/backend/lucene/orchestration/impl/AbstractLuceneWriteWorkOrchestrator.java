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
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
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
	public CompletableFuture<?> submit(List<LuceneWriteWork<?>> works,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy) {
		CompletableFuture<Object> future = new CompletableFuture<>();
		submit( new LuceneMultipleWorkSet( works, future, commitStrategy, refreshStrategy ) );
		return future;
	}

	@Override
	public <T> CompletableFuture<T> submit(LuceneWriteWork<T> work,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy) {
		CompletableFuture<T> future = new CompletableFuture<>();
		submit( new LuceneSingleWorkSet<>( work, future, commitStrategy, refreshStrategy ) );
		return future;
	}

	interface LuceneWorkSet extends BatchingExecutor.WorkSet<LuceneWriteWorkProcessor> {
	}

	static class LuceneMultipleWorkSet implements LuceneWorkSet {
		private final List<LuceneWriteWork<?>> works;
		private final CompletableFuture<Object> future;
		private final DocumentCommitStrategy commitStrategy;
		private final DocumentRefreshStrategy refreshStrategy;

		LuceneMultipleWorkSet(List<LuceneWriteWork<?>> works, CompletableFuture<Object> future,
				DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy) {
			this.works = new ArrayList<>( works );
			this.future = future;
			this.commitStrategy = commitStrategy;
			this.refreshStrategy = refreshStrategy;
		}

		@Override
		public void submitTo(LuceneWriteWorkProcessor processor) {
			processor.beforeWorkSet( commitStrategy, refreshStrategy );
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
		private final DocumentCommitStrategy commitStrategy;
		private final DocumentRefreshStrategy refreshStrategy;

		LuceneSingleWorkSet(LuceneWriteWork<T> work, CompletableFuture<T> future,
				DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy) {
			this.work = work;
			this.future = future;
			this.commitStrategy = commitStrategy;
			this.refreshStrategy = refreshStrategy;
		}

		@Override
		public void submitTo(LuceneWriteWorkProcessor processor) {
			processor.beforeWorkSet( commitStrategy, refreshStrategy );
			T result = processor.submit( work );
			processor.afterWorkSet( future, result );
		}

		@Override
		public void markAsFailed(Throwable t) {
			future.completeExceptionally( t );
		}
	}

}
