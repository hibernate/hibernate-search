/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.orchestration.spi;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.CompletableFuture;
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
public abstract class AbstractWorkOrchestrator<W> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final String name;

	private State state = State.STOPPED; // Guarded by lifecycleLock
	private final ReadWriteLock lifecycleLock = new ReentrantReadWriteLock();

	protected AbstractWorkOrchestrator(String name) {
		this.name = name;
	}

	protected final String getName() {
		return name;
	}

	public final void start() {
		lifecycleLock.writeLock().lock();
		try {
			switch ( state ) {
				case RUNNING:
					return;
				case PRE_STOPPING:
					throw new IllegalStateException( "Cannot start an orchestrator while it's stopping" );
				case STOPPED:
					state = State.RUNNING;
					doStart();
					break;
			}
		}
		finally {
			lifecycleLock.writeLock().unlock();
		}
	}

	public final CompletableFuture<?> preStop() {
		lifecycleLock.writeLock().lock();
		try {
			switch ( state ) {
				case RUNNING:
					state = State.PRE_STOPPING;
					return getCompletion();
				case PRE_STOPPING:
				case STOPPED:
				default:
					return getCompletion();
			}
		}
		finally {
			lifecycleLock.writeLock().unlock();
		}
	}

	public final void stop() {
		lifecycleLock.writeLock().lock();
		try {
			switch ( state ) {
				case RUNNING:
				case PRE_STOPPING:
					state = State.STOPPED;
					doStop();
					break;
				case STOPPED:
					break;
			}
		}
		finally {
			lifecycleLock.writeLock().unlock();
		}
	}

	protected abstract void doStart();

	protected abstract void doSubmit(W workSet) throws InterruptedException;

	protected abstract CompletableFuture<?> getCompletion();

	protected abstract void doStop();

	protected final void submit(W workSet) {
		if ( !lifecycleLock.readLock().tryLock() ) {
			// The orchestrator is starting, pre-stopping or stopping: abort.
			throw log.submittedWorkToStoppedOrchestrator( name );
		}
		try {
			if ( !State.RUNNING.equals( state ) ) {
				// The orchestrator is stopping or stopped: abort.
				throw log.submittedWorkToStoppedOrchestrator( name );
			}
			doSubmit( workSet );
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw log.threadInterruptedWhileSubmittingWorkset( name );
		}
		finally {
			lifecycleLock.readLock().unlock();
		}
	}

	private enum State {
		RUNNING,
		PRE_STOPPING,
		STOPPED;
	}

}
