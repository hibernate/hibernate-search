/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.processor.impl;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.hibernate.search.elasticsearch.logging.impl.Log;
import org.hibernate.search.elasticsearch.work.impl.ElasticsearchWork;
import org.hibernate.search.util.logging.impl.LoggerFactory;


/**
 * An abstract base for {@link BarrierElasticsearchWorkOrchestrator} implementations,
 * implementing a thread-safe shutdown.
 *
 * @author Yoann Rodiere
 */
abstract class AbstractBarrierElasticsearchWorkOrchestrator
		implements BarrierElasticsearchWorkOrchestrator, AutoCloseable {

	private static final Log LOG = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final String name;

	private boolean open = true; // Guarded by shutdownLock
	private final ReadWriteLock shutdownLock = new ReentrantReadWriteLock();

	protected AbstractBarrierElasticsearchWorkOrchestrator(String name) {
		this.name = name;
	}

	protected final String getName() {
		return name;
	}

	@Override
	public CompletableFuture<Void> submit(Iterable<ElasticsearchWork<?>> works) {
		if ( !shutdownLock.readLock().tryLock() ) {
			// The orchestrator is shutting down: abort.
			throw LOG.orchestratorShutDownBeforeSubmittingChangeset( name );
		}
		try {
			if ( !open ) {
				// The orchestrator has shut down: abort.
				throw LOG.orchestratorShutDownBeforeSubmittingChangeset( name );
			}
			return doSubmit( works );
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw LOG.threadInterruptedWhileSubmittingChangeset( name );
		}
		finally {
			shutdownLock.readLock().unlock();
		}
	}

	protected abstract CompletableFuture<Void> doSubmit(Iterable<ElasticsearchWork<?>> works) throws InterruptedException;

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
