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

import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * An abstract base for orchestrator implementations,
 * implementing a thread-safe shutdown.
 *
 * @param <W> The type of batched works.
 */
public abstract class AbstractWorkOrchestrator<W> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final String name;

	private State state = State.STOPPED; // Guarded by lifecycleLock
	private final ReadWriteLock lifecycleLock = new ReentrantReadWriteLock();

	protected AbstractWorkOrchestrator(String name) {
		this.name = name;
	}

	protected final String name() {
		return name;
	}

	/**
	 * Start any resource necessary to operate the orchestrator at runtime.
	 * <p>
	 * Called by the owner of this orchestrator once after bootstrap,
	 * before any other method is called.
	 *
	 * @param propertySource The property source to extract configuration from.
	 */
	public final void start(ConfigurationPropertySource propertySource) {
		lifecycleLock.writeLock().lock();
		try {
			switch ( state ) {
				case RUNNING:
					return;
				case PRE_STOPPING:
					throw new IllegalStateException( "Cannot start an orchestrator while it's stopping" );
				case STOPPED:
					state = State.RUNNING;
					doStart( propertySource );
					break;
			}
		}
		finally {
			lifecycleLock.writeLock().unlock();
		}
	}

	/**
	 * Stop accepting works and return a future that completes when all works have been completely executed.
	 * <p>
	 * Optionally called by the owner of this orchestrator before {@link #stop()},
	 * if it needs to wait for work completion.
	 *
	 * @return A future that completes when all ongoing works have been completely executed.
	 */
	public final CompletableFuture<?> preStop() {
		lifecycleLock.writeLock().lock();
		try {
			switch ( state ) {
				case RUNNING:
					state = State.PRE_STOPPING;
					return completion();
				case PRE_STOPPING:
				case STOPPED:
				default:
					return completion();
			}
		}
		finally {
			lifecycleLock.writeLock().unlock();
		}
	}

	/**
	 * Forcibly shut down ongoing work and release any resource necessary to operate the orchestrator at runtime.
	 * <p>
	 * Called by the owner of this orchestrator on shutdown.
	 */
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

	protected abstract void doStart(ConfigurationPropertySource propertySource);

	protected abstract void doSubmit(W work, OperationSubmitter operationSubmitter) throws InterruptedException;

	protected abstract CompletableFuture<?> completion();

	protected abstract void doStop();

	public final void submit(W work, OperationSubmitter operationSubmitter) {
		if ( !lifecycleLock.readLock().tryLock() ) {
			// The orchestrator is starting, pre-stopping or stopping: abort.
			throw log.submittedWorkToStoppedOrchestrator( name );
		}
		try {
			if ( !State.RUNNING.equals( state ) ) {
				// The orchestrator is stopping or stopped: abort.
				throw log.submittedWorkToStoppedOrchestrator( name );
			}
			doSubmit( work, operationSubmitter );
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw log.threadInterruptedWhileSubmittingWork( name );
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
