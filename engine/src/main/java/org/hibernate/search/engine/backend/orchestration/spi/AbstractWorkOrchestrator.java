/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.orchestration.spi;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * An abstract base for orchestrator implementations,
 * implementing a thread-safe shutdown.
 *
 * @param <W> The type of submitted worksets.
 */
public abstract class AbstractWorkOrchestrator<W> implements AutoCloseable {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final String name;

	private boolean open = true; // Guarded by shutdownLock
	private final ReadWriteLock shutdownLock = new ReentrantReadWriteLock();

	protected AbstractWorkOrchestrator(String name) {
		this.name = name;
	}

	protected final String getName() {
		return name;
	}

	@Override
	public final void close() {
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

	protected abstract void doSubmit(W workSet) throws InterruptedException;

	protected abstract void doClose();

	protected final void submit(W workSet) {
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

}
