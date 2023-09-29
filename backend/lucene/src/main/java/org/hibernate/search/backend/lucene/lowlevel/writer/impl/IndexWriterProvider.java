/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.writer.impl;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.lowlevel.directory.spi.DirectoryHolder;
import org.hibernate.search.backend.lucene.resources.impl.BackendThreads;
import org.hibernate.search.engine.common.timing.spi.TimingSource;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reporting.EventContext;

import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.MergeScheduler;

/**
 * @author Sanne Grinovero (C) 2011 Red Hat Inc.
 */
public class IndexWriterProvider {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final String indexName;
	private final EventContext eventContext;
	private final DirectoryHolder directoryHolder;
	private final IndexWriterConfigSource configSource;
	private final TimingSource timingSource;
	private final int commitInterval;
	private BackendThreads threads;
	private final FailureHandler failureHandler;

	/**
	 * Current open IndexWriter, or null when closed.
	 */
	private final AtomicReference<IndexWriterDelegatorImpl> currentWriter = new AtomicReference<>();

	/**
	 * Protects from multiple initialization attempts of IndexWriter
	 */
	private final ReentrantLock currentWriterModificationLock = new ReentrantLock();

	public IndexWriterProvider(String indexName, EventContext eventContext,
			DirectoryHolder directoryHolder, IndexWriterConfigSource configSource,
			TimingSource timingSource, int commitInterval,
			BackendThreads threads,
			FailureHandler failureHandler) {
		this.indexName = indexName;
		this.eventContext = eventContext;
		this.directoryHolder = directoryHolder;
		this.configSource = configSource;
		this.timingSource = timingSource;
		this.commitInterval = commitInterval;
		this.threads = threads;
		this.failureHandler = failureHandler;
	}

	/**
	 * Closes and drops any cached resources (index writer in particular).
	 * <p>
	 * Should be used when stopping the index.
	 */
	public void clear() throws IOException {
		currentWriterModificationLock.lock();
		try {
			IndexWriterDelegatorImpl indexWriterDelegator = currentWriter.getAndSet( null );
			if ( indexWriterDelegator != null ) {
				indexWriterDelegator.close();
			}
		}
		finally {
			currentWriterModificationLock.unlock();
		}
	}

	/**
	 * Closes and drops any cached resources (index writer in particular).
	 * <p>
	 * Should be used to clean up upon error.
	 */
	public void clearAfterFailure(Throwable throwable, Object failingOperation) {
		log.indexWriterResetAfterFailure( eventContext );

		/*
		 * Acquire the lock so that we're sure no writer will be created for the directory before we close the current one.
		 * This means in particular that write locks to the directory will be released,
		 * at least for a short period of time.
		 */
		currentWriterModificationLock.lock();
		IndexWriterDelegatorImpl indexWriterDelegator;
		try {
			indexWriterDelegator = currentWriter.getAndSet( null );
			if ( indexWriterDelegator != null ) {
				indexWriterDelegator.closeAfterFailure( throwable, failingOperation );
			}
		}
		finally {
			currentWriterModificationLock.unlock();
		}
	}

	public IndexWriterDelegatorImpl getOrNull() {
		return currentWriter.get();
	}

	public IndexWriterDelegatorImpl getOrCreate() throws IOException {
		IndexWriterDelegatorImpl indexWriterDelegator = currentWriter.get();
		if ( indexWriterDelegator == null ) {
			currentWriterModificationLock.lock();
			try {
				indexWriterDelegator = currentWriter.get();
				if ( indexWriterDelegator == null ) {
					IndexWriter indexWriter = createNewIndexWriter();
					indexWriterDelegator = new IndexWriterDelegatorImpl(
							indexWriter, eventContext,
							threads.getWriteExecutor(),
							timingSource, commitInterval,
							failureHandler,
							this::clearAfterFailure
					);
					log.trace( "IndexWriter opened" );
					currentWriter.set( indexWriterDelegator );
				}
			}
			finally {
				currentWriterModificationLock.unlock();
			}
		}
		return indexWriterDelegator;
	}

	private IndexWriter createNewIndexWriter() throws IOException {
		// Each writer config can be attached only once to an IndexWriter
		final IndexWriterConfig indexWriterConfig = createWriterConfig();
		return new IndexWriter( directoryHolder.get(), indexWriterConfig );
	}

	private IndexWriterConfig createWriterConfig() {
		IndexWriterConfig writerConfig = configSource.createIndexWriterConfig();
		MergeScheduler mergeScheduler = new HibernateSearchConcurrentMergeScheduler(
				indexName, eventContext.render(),
				threads.getThreadProvider(), failureHandler
		);
		writerConfig.setMergeScheduler( mergeScheduler );
		writerConfig.setOpenMode( OpenMode.CREATE_OR_APPEND );
		return writerConfig;
	}
}
