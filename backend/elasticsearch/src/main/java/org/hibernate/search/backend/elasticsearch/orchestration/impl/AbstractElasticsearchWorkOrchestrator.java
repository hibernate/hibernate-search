/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.orchestration.impl;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchWork;
import org.hibernate.search.engine.backend.orchestration.spi.BatchingExecutor;
import org.hibernate.search.util.common.impl.Futures;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * An abstract base for {@link ElasticsearchWorkOrchestratorImplementor} implementations,
 * implementing a thread-safe shutdown.
 *
 * @author Yoann Rodiere
 */
abstract class AbstractElasticsearchWorkOrchestrator
		implements ElasticsearchWorkOrchestratorImplementor, AutoCloseable {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final String name;

	private boolean open = true; // Guarded by shutdownLock
	private final ReadWriteLock shutdownLock = new ReentrantReadWriteLock();

	protected AbstractElasticsearchWorkOrchestrator(String name) {
		this.name = name;
	}

	protected final String getName() {
		return name;
	}

	@Override
	public CompletableFuture<?> submit(List<ElasticsearchWork<?>> works) {
		CompletableFuture<Object> future = new CompletableFuture<>();
		submit( new ElasticsearchMultipleWorkSet( works, future ) );
		return future;
	}

	@Override
	public <T> CompletableFuture<T> submit(ElasticsearchWork<T> work) {
		CompletableFuture<T> future = new CompletableFuture<>();
		submit( new ElasticsearchSingleWorkSet<>( work, future ) );
		return future;
	}

	@Override
	public void close() {
		shutdownLock.writeLock().lock();
		try {
			if ( !open ) {
				return;
			}
			open = false;
			doClose();
		}
		finally {
			shutdownLock.writeLock().unlock();
		}
	}

	protected abstract void doSubmit(ElasticsearchWorkSet workSet) throws InterruptedException;

	protected abstract void doClose();

	void submit(ElasticsearchWorkSet workSet) {
		if ( !shutdownLock.readLock().tryLock() ) {
			// The orchestrator is shutting down: abort.
			throw log.orchestratorShutDownBeforeSubmittingWorkset( name );
		}
		try {
			if ( !open ) {
				// The orchestrator has shut down: abort.
				throw log.orchestratorShutDownBeforeSubmittingWorkset( name );
			}
			doSubmit( workSet );
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw log.threadInterruptedWhileSubmittingWorkset( name );
		}
		finally {
			shutdownLock.readLock().unlock();
		}
	}

	interface ElasticsearchWorkSet extends BatchingExecutor.Task<ElasticsearchWorkOrchestrationStrategy> {
	}

	static class ElasticsearchMultipleWorkSet implements ElasticsearchWorkSet {
		private final List<ElasticsearchWork<?>> works;
		private final CompletableFuture<Object> future;

		ElasticsearchMultipleWorkSet(List<ElasticsearchWork<?>> works, CompletableFuture<Object> future) {
			this.works = new ArrayList<>( works );
			this.future = future;
		}

		@Override
		public void submitTo(ElasticsearchWorkOrchestrationStrategy delegate) {
			delegate.submit( works ).whenComplete( Futures.copyHandler( future ) );
		}

		@Override
		public void markAsFailed(Throwable t) {
			future.completeExceptionally( t );
		}
	}

	static class ElasticsearchSingleWorkSet<T> implements ElasticsearchWorkSet {
		private final ElasticsearchWork<T> work;
		private final CompletableFuture<T> future;

		ElasticsearchSingleWorkSet(ElasticsearchWork<T> work, CompletableFuture<T> future) {
			this.work = work;
			this.future = future;
		}

		@Override
		public void submitTo(ElasticsearchWorkOrchestrationStrategy delegate) {
			delegate.submit( work ).whenComplete( Futures.copyHandler( future ) );
		}

		@Override
		public void markAsFailed(Throwable t) {
			future.completeExceptionally( t );
		}
	}

}
