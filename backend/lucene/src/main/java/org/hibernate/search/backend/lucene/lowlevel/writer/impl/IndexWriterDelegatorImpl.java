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
import org.hibernate.search.engine.common.spi.ErrorHandler;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reporting.EventContext;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.MergeScheduler;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.Directory;

/**
 * @author Sanne Grinovero (C) 2011 Red Hat Inc.
 */
public class IndexWriterDelegatorImpl implements Closeable, IndexWriterDelegator {
	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final String indexName;
	private final EventContext indexEventContext;
	private final Directory directory;
	private final Analyzer analyzer;
	private final ErrorHandler errorHandler;

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

	public IndexWriterDelegatorImpl(String indexName, Directory directory, Analyzer analyzer, ErrorHandler errorHandler) {
		this.indexName = indexName;
		this.indexEventContext = EventContexts.fromIndexName( indexName );
		// TODO HSEARCH-3440 use our own SPI instead of a directory directly
		this.directory = directory;
		this.analyzer = analyzer;
		this.errorHandler = errorHandler;
		/* TODO HSEARCH-3117 re-allow to configure index writers
		this.luceneParameters = indexManager.getIndexingParameters();
		this.indexParameters = luceneParameters.getIndexParameters();
		this.similarity = indexManager.getSimilarity();
		 */
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
		return new IndexWriter( directory, indexWriterConfig );
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
		MergeScheduler mergeScheduler = new HibernateSearchConcurrentMergeScheduler( this.errorHandler, this.indexName );
		writerConfig.setMergeScheduler( mergeScheduler );
		writerConfig.setOpenMode( OpenMode.CREATE_OR_APPEND );
		return writerConfig;
	}

}
