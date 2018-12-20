/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.orchestration.impl;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchWork;
import org.hibernate.search.util.impl.common.LoggerFactory;


/**
 * An abstract base for {@link ElasticsearchBarrierWorkOrchestrator} implementations,
 * implementing a thread-safe shutdown.
 *
 * @author Yoann Rodiere
 */
abstract class AbstractElasticsearchBarrierWorkOrchestrator
		implements ElasticsearchBarrierWorkOrchestrator, AutoCloseable {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final String name;

	private boolean open = true; // Guarded by shutdownLock
	private final ReadWriteLock shutdownLock = new ReentrantReadWriteLock();

	protected AbstractElasticsearchBarrierWorkOrchestrator(String name) {
		this.name = name;
	}

	protected final String getName() {
		return name;
	}

	@Override
	public CompletableFuture<?> submit(List<ElasticsearchWork<?>> works) {
		if ( !shutdownLock.readLock().tryLock() ) {
			// The orchestrator is shutting down: abort.
			throw log.orchestratorShutDownBeforeSubmittingChangeset( name );
		}
		try {
			if ( !open ) {
				// The orchestrator has shut down: abort.
				throw log.orchestratorShutDownBeforeSubmittingChangeset( name );
			}
			return doSubmit( works );
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw log.threadInterruptedWhileSubmittingChangeset( name );
		}
		finally {
			shutdownLock.readLock().unlock();
		}
	}

	@Override
	public <T> CompletableFuture<T> submit(ElasticsearchWork<T> work) {
		throw new UnsupportedOperationException( "Single work execution is not supported yet!" );
	}

	protected abstract CompletableFuture<?> doSubmit(List<ElasticsearchWork<?>> works) throws InterruptedException;

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

	protected abstract void doClose();

}
