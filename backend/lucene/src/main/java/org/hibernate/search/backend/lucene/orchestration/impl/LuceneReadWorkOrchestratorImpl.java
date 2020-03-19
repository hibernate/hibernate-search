/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.orchestration.impl;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.lowlevel.reader.impl.HibernateSearchMultiReader;
import org.hibernate.search.backend.lucene.lowlevel.reader.impl.IndexReaderMetadataResolver;
import org.hibernate.search.backend.lucene.lowlevel.reader.impl.ReadIndexManagerContext;
import org.hibernate.search.backend.lucene.work.impl.ReadWork;
import org.hibernate.search.backend.lucene.work.impl.ReadWorkExecutionContext;
import org.hibernate.search.engine.backend.orchestration.spi.AbstractWorkOrchestrator;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.impl.SuppressingCloser;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reporting.EventContext;

import org.apache.lucene.index.IndexReader;

/**
 * An orchestrator that executes read works synchronously in the current thread.
 * <p>
 * For now this implementation is very simple,
 * but we might one day need to execute queries asynchronously,
 * in which case thing will get slightly more complex.
 */
public class LuceneReadWorkOrchestratorImpl
		extends AbstractWorkOrchestrator<LuceneReadWorkOrchestratorImpl.WorkExecution<?>>
		implements LuceneReadWorkOrchestratorImplementor {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	public LuceneReadWorkOrchestratorImpl(String name) {
		super( name );
		start(); // Nothing to start, just force the superclass to go to the right state.
	}

	@Override
	public <T> T submit(Set<String> indexNames, Set<? extends ReadIndexManagerContext> indexManagerContexts,
			Set<String> routingKeys, ReadWork<T> work) {
		WorkExecution<T> workExecution = new WorkExecution<>( indexNames, indexManagerContexts, routingKeys, work );
		Throwable throwable = null;
		try {
			submit( workExecution );
			// If we get there, the task succeeded and we are sure there is a result.
			return workExecution.getResult();
		}
		catch (Throwable t) {
			// Just remember something went wrong
			throwable = t;
			throw t;
		}
		finally {
			if ( throwable == null ) {
				workExecution.close();
			}
			else {
				// Take care not to erase the main error if closing the context fails: use addSuppressed() instead
				new SuppressingCloser( throwable )
						.push( workExecution );
			}
		}
	}

	@Override
	protected void doStart() {
		// Nothing to do
	}

	@Override
	protected void doSubmit(WorkExecution<?> work) {
		work.execute();
	}

	@Override
	protected CompletableFuture<?> getCompletion() {
		// Works are executed synchronously.
		return CompletableFuture.completedFuture( null );
	}

	@Override
	protected void doStop() {
		// Nothing to do
	}

	static class WorkExecution<T> implements AutoCloseable, ReadWorkExecutionContext {
		private final Set<String> indexNames;
		private final HibernateSearchMultiReader indexReader;
		private final ReadWork<T> work;

		private T result;

		WorkExecution(Set<String> indexNames, Set<? extends ReadIndexManagerContext> indexManagerContexts,
				Set<String> routingKeys, ReadWork<T> work) {
			this.indexNames = indexNames;
			this.indexReader = HibernateSearchMultiReader.open( indexNames, indexManagerContexts, routingKeys );
			this.work = work;
		}

		@Override
		public IndexReader getIndexReader() {
			return indexReader;
		}

		@Override
		public IndexReaderMetadataResolver getIndexReaderMetadataResolver() {
			return indexReader.getMetadataResolver();
		}

		@Override
		public EventContext getEventContext() {
			return EventContexts.fromIndexNames( indexNames );
		}

		public void execute() {
			result = work.execute( this );
		}

		public T getResult() {
			return result;
		}

		@Override
		public void close() {
			try {
				indexReader.close();
			}
			catch (IOException | RuntimeException e) {
				log.unableToCloseIndexReader( getEventContext(), e );
			}
		}
	}

}
