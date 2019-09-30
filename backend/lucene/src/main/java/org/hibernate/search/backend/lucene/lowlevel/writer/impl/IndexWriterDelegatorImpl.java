/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.writer.impl;

import java.io.Closeable;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.lowlevel.directory.spi.DirectoryHolder;
import org.hibernate.search.backend.lucene.util.impl.AnalyzerConstants;
import org.hibernate.search.engine.common.spi.ErrorHandler;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reporting.EventContext;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.MergeScheduler;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.store.SleepingLockWrapper;

/**
 * @author Sanne Grinovero (C) 2011 Red Hat Inc.
 */
public class IndexWriterDelegatorImpl implements Closeable, IndexWriterDelegator {
	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final DirectoryHolder directoryHolder;
	private final Analyzer analyzer;
	private final ErrorHandler errorHandler;
	private final EventContext indexEventContext;

	/* TODO HSEARCH-3117 re-allow to configure index writers
	private final Similarity similarity;
	private final LuceneIndexingParameters luceneParameters;
	private final ParameterSet indexParameters;
	 */

	/**
	 * Current open IndexWriter, or null when closed.
	 */
	private final AtomicReference<IndexWriter> writer = new AtomicReference<>();

	/**
	 * Protects from multiple initialization attempts of IndexWriter
	 */
	private final ReentrantLock writerInitializationLock = new ReentrantLock();

	public IndexWriterDelegatorImpl(DirectoryHolder directoryHolder, Analyzer analyzer,
			ErrorHandler errorHandler, EventContext eventContext) {
		this.directoryHolder = directoryHolder;
		this.analyzer = analyzer;
		this.errorHandler = errorHandler;
		this.indexEventContext = eventContext;
		/* TODO HSEARCH-3117 re-allow to configure index writers
		this.luceneParameters = indexManager.getIndexingParameters();
		this.indexParameters = luceneParameters.getIndexParameters();
		this.similarity = indexManager.getSimilarity();
		 */
	}

	@Override
	public void ensureIndexExists() throws IOException {
		Directory directory = directoryHolder.get();

		if ( DirectoryReader.indexExists( directory ) ) {
			return;
		}

		try {
			IndexWriterConfig iwriterConfig = new IndexWriterConfig( AnalyzerConstants.KEYWORD_ANALYZER )
					.setOpenMode( IndexWriterConfig.OpenMode.CREATE_OR_APPEND );
			//Needs to have a timeout higher than zero to prevent race conditions over (network) RPCs
			//for distributed indexes (Infinispan but probably also NFS and similar)
			SleepingLockWrapper delayedDirectory = new SleepingLockWrapper( directory, 2000, 20 );
			IndexWriter iw = new IndexWriter( delayedDirectory, iwriterConfig );
			iw.close();
		}
		catch (LockObtainFailedException lofe) {
			log.lockingFailureDuringInitialization( directory.toString(), indexEventContext );
		}
	}

	@Override
	public void close() throws IOException {
		final IndexWriter toClose = writer.getAndSet( null );
		if ( toClose != null ) {
			try {
				toClose.close();
				log.trace( "IndexWriter closed" );
			}
			catch (IOException ioe) {
				forceLockRelease();
				throw ioe;
			}
		}
	}

	@Override
	public long addDocuments(Iterable<? extends Iterable<? extends IndexableField>> docs) throws IOException {
		return getOrCreateIndexWriter().addDocuments( docs );
	}

	@Override
	public long updateDocuments(Term term, Iterable<? extends Iterable<? extends IndexableField>> docs) throws IOException {
		return getOrCreateIndexWriter().updateDocuments( term, docs );
	}

	@Override
	public long deleteDocuments(Term term) throws IOException {
		return getOrCreateIndexWriter().deleteDocuments( term );
	}

	@Override
	public long deleteDocuments(Query query) throws IOException {
		return getOrCreateIndexWriter().deleteDocuments( query );
	}

	@Override
	public long deleteAll() throws IOException {
		return getOrCreateIndexWriter().deleteAll();
	}

	@Override
	public void commit() throws IOException {
		getOrCreateIndexWriter().commit();
	}

	@Override
	public void flush() throws IOException {
		getOrCreateIndexWriter().flush();
	}

	@Override
	public void forceMerge() throws IOException {
		getOrCreateIndexWriter().forceMerge( 1 );
	}

	@Override
	public void forceLockRelease() throws IOException {
		log.forcingReleaseIndexWriterLock( indexEventContext );
		/*
		 * Acquire the lock so that we're sure no writer will be created for the directory before we close the current one.
		 * This means in particular that write locks to the directory will be released,
		 * at least for a short period of time.
		 */
		writerInitializationLock.lock();
		try {
			IndexWriter indexWriter = writer.getAndSet( null );
			if ( indexWriter != null ) {
				indexWriter.close();
				log.trace( "IndexWriter closed" );
			}
		}
		finally {
			writerInitializationLock.unlock();
		}
	}

	public IndexWriter getIndexWriterOrNull() {
		return writer.get();
	}

	/**
	 * Gets the IndexWriter, opening one if needed.
	 *
	 * @return a new IndexWriter or one already open.
	 */
	private IndexWriter getOrCreateIndexWriter() throws IOException {
		IndexWriter indexWriter = writer.get();
		if ( indexWriter == null ) {
			writerInitializationLock.lock();
			try {
				indexWriter = writer.get();
				if ( indexWriter == null ) {
					indexWriter = createNewIndexWriter();
					log.trace( "IndexWriter opened" );
					writer.set( indexWriter );
				}
			}
			finally {
				writerInitializationLock.unlock();
			}
		}
		return indexWriter;
	}

	private IndexWriter createNewIndexWriter() throws IOException {
		// Each writer config can be attached only once to an IndexWriter
		final IndexWriterConfig indexWriterConfig = createWriterConfig();
		return new IndexWriter( directoryHolder.get(), indexWriterConfig );
	}

	private IndexWriterConfig createWriterConfig() {
		IndexWriterConfig writerConfig = new IndexWriterConfig( analyzer );
		/* TODO HSEARCH-3117 re-allow to configure index writers
		luceneParameters.applyToWriter( writerConfig );
		if ( similarity != null ) {
			writerConfig.setSimilarity( similarity );
		}
		LogByteSizeMergePolicy newMergePolicy = indexParameters.getNewMergePolicy(); //TODO HSEARCH-3117 make it possible to configure a different policy?
		writerConfig.setMergePolicy( newMergePolicy );
		 */
		MergeScheduler mergeScheduler = new HibernateSearchConcurrentMergeScheduler( this.errorHandler, indexEventContext.render() );
		writerConfig.setMergeScheduler( mergeScheduler );
		writerConfig.setOpenMode( OpenMode.CREATE_OR_APPEND );
		return writerConfig;
	}

}
