/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.orchestration.impl;

import java.io.IOException;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.lucene.cache.impl.LuceneQueryCachingContext;
import org.hibernate.search.backend.lucene.logging.impl.LuceneMiscLog;
import org.hibernate.search.backend.lucene.logging.impl.QueryLog;
import org.hibernate.search.backend.lucene.lowlevel.reader.impl.HibernateSearchMultiReader;
import org.hibernate.search.backend.lucene.lowlevel.reader.impl.IndexReaderMetadataResolver;
import org.hibernate.search.backend.lucene.lowlevel.reader.impl.ReadIndexManagerContext;
import org.hibernate.search.backend.lucene.work.impl.ReadWork;
import org.hibernate.search.backend.lucene.work.impl.ReadWorkExecutionContext;
import org.hibernate.search.engine.backend.orchestration.spi.AbstractWorkOrchestrator;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.impl.SuppressingCloser;
import org.hibernate.search.util.common.reporting.EventContext;

import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.similarities.Similarity;

public class LuceneSyncWorkOrchestratorImpl
		extends AbstractWorkOrchestrator<LuceneSyncWorkOrchestratorImpl.WorkExecution<?>>
		implements LuceneSyncWorkOrchestrator {

	private final Similarity similarity;
	private final LuceneQueryCachingContext cachingContext;

	public LuceneSyncWorkOrchestratorImpl(String name, Similarity similarity,
			LuceneQueryCachingContext cachingContext) {
		super( name );
		this.similarity = similarity;
		start( null ); // Nothing to start, just force the superclass to go to the right state.
		this.cachingContext = cachingContext;
	}

	@Override
	public <T> T submit(Set<String> indexNames, Collection<? extends ReadIndexManagerContext> indexManagerContexts,
			Set<String> routingKeys, ReadWork<T> work,
			HibernateSearchMultiReader indexReader) {
		WorkExecution<T> workExecution = new WorkExecution<>(
				similarity, indexNames, indexManagerContexts, routingKeys, work, indexReader, cachingContext
		);
		Throwable throwable = null;
		try {
			submit( workExecution, OperationSubmitter.blocking() );
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
	protected void doStart(ConfigurationPropertySource propertySource) {
		// Nothing to do
	}

	@Override
	protected void doSubmit(WorkExecution<?> work, OperationSubmitter operationSubmitter) {
		if ( !OperationSubmitter.blocking().equals( operationSubmitter ) ) {
			throw LuceneMiscLog.INSTANCE.nonblockingOperationSubmitterNotSupported();
		}
		work.execute();
	}

	@Override
	protected CompletableFuture<?> completion() {
		// Works are executed synchronously.
		return CompletableFuture.completedFuture( null );
	}

	@Override
	protected void doStop() {
		// Nothing to do
	}

	static class WorkExecution<T> implements AutoCloseable, ReadWorkExecutionContext {
		private final Similarity similarity;
		private final Set<String> indexNames;
		private final HibernateSearchMultiReader indexReader;
		private final ReadWork<T> work;
		private final boolean closeIndexReader;
		private final LuceneQueryCachingContext cachingContext;

		private T result;

		WorkExecution(Similarity similarity, Set<String> indexNames,
				Collection<? extends ReadIndexManagerContext> indexManagerContexts,
				Set<String> routingKeys, ReadWork<T> work,
				HibernateSearchMultiReader indexReader,
				LuceneQueryCachingContext cachingContext) {
			this.similarity = similarity;
			this.indexNames = indexNames;
			this.work = work;

			if ( indexReader == null ) {
				this.indexReader = HibernateSearchMultiReader.open( indexNames, indexManagerContexts, routingKeys );
				this.closeIndexReader = true;
			}
			else {
				this.indexReader = indexReader;
				this.closeIndexReader = false;
			}
			this.cachingContext = cachingContext;
		}

		@Override
		public IndexSearcher createSearcher() {
			IndexSearcher searcher = new IndexSearcher( indexReader );
			searcher.setSimilarity( similarity );

			cachingContext.queryCache().ifPresent( searcher::setQueryCache );
			if ( cachingContext.queryCachingPolicy().isPresent() ) {
				searcher.setQueryCachingPolicy( cachingContext.queryCachingPolicy().get() );
				// Note: Lucene 11 will not enable cache by default so policy won't get applied, let's warn users if this happens:
				if ( searcher.getQueryCache() == null ) {
					QueryLog.INSTANCE.ineffectiveQueryCachingPolicy();
				}
			}

			return searcher;
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
			if ( !closeIndexReader ) {
				return;
			}

			try {
				indexReader.close();
			}
			catch (IOException | RuntimeException e) {
				LuceneMiscLog.INSTANCE.unableToCloseIndexReader( getEventContext(), e );
			}
		}
	}

}
