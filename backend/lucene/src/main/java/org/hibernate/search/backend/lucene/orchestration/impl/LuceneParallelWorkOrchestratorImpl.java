/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.orchestration.impl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import org.hibernate.search.backend.lucene.lowlevel.index.impl.IndexAccessor;
import org.hibernate.search.backend.lucene.resources.impl.BackendThreads;
import org.hibernate.search.backend.lucene.work.impl.IndexManagementWork;
import org.hibernate.search.backend.lucene.work.impl.IndexManagementWorkExecutionContext;
import org.hibernate.search.engine.backend.orchestration.spi.AbstractWorkOrchestrator;
import org.hibernate.search.engine.cfg.spi.ConfigurationPropertySource;
import org.hibernate.search.util.common.reporting.EventContext;

public class LuceneParallelWorkOrchestratorImpl
		extends AbstractWorkOrchestrator<LuceneParallelWorkOrchestratorImpl.WorkExecution<?>>
		implements LuceneParallelWorkOrchestrator {

	private final IndexAccessor indexAccessor;
	private final IndexAccessorWorkExecutionContext context;
	private final BackendThreads threads;

	private ExecutorService executor;

	public LuceneParallelWorkOrchestratorImpl(String name,
			EventContext eventContext, IndexAccessor indexAccessor,
			BackendThreads threads) {
		super( name );
		this.indexAccessor = indexAccessor;
		this.context = new IndexAccessorWorkExecutionContext( eventContext, indexAccessor );
		this.threads = threads;
	}

	@Override
	public <T> void submit(CompletableFuture<T> future, IndexManagementWork<T> work) {
		submit( new WorkExecution<>( future, work, context ) );
	}

	@Override
	public void forceCommitInCurrentThread() {
		try {
			indexAccessor.commit();
		}
		catch (Throwable e) {
			context.getIndexAccessor().cleanUpAfterFailure( e, "Commit after an index management operation" );
			throw e;
		}
	}

	@Override
	protected void doStart(ConfigurationPropertySource propertySource) {
		executor = threads.getWriteExecutor();
	}

	@Override
	protected void doSubmit(WorkExecution<?> workExecution) {
		executor.submit( workExecution );
	}

	@Override
	protected CompletableFuture<?> completion() {
		// We do not wait for these works to finish;
		// callers were provided with a future and are responsible for waiting
		// before they close the application.
		return CompletableFuture.completedFuture( null );
	}

	@Override
	protected void doStop() {
		executor = null;
	}

	static class WorkExecution<T> implements Runnable {
		private final CompletableFuture<T> result;
		private final IndexManagementWork<T> work;
		private final IndexManagementWorkExecutionContext context;

		WorkExecution(CompletableFuture<T> result, IndexManagementWork<T> work,
				IndexManagementWorkExecutionContext context) {
			this.result = result;
			this.work = work;
			this.context = context;
		}

		@Override
		public void run() {
			try {
				result.complete( work.execute( context ) );
			}
			catch (Throwable e) {
				context.getIndexAccessor().cleanUpAfterFailure( e, work.getInfo() );
				result.completeExceptionally( e );
			}
		}

		public CompletableFuture<T> getResult() {
			return result;
		}
	}

}
