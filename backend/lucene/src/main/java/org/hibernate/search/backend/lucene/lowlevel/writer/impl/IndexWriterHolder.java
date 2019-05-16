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
import org.hibernate.search.engine.common.spi.ErrorContext;
import org.hibernate.search.engine.common.spi.ErrorContextBuilder;
import org.hibernate.search.engine.common.spi.ErrorHandler;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.MergeScheduler;
import org.apache.lucene.store.Directory;

/**
 * @author Sanne Grinovero (C) 2011 Red Hat Inc.
 */
class IndexWriterHolder {
	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final String indexName;
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

	IndexWriterHolder(String indexName, Directory directory, Analyzer analyzer, ErrorHandler errorHandler) {
		this.indexName = indexName;
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

	/**
	 * Gets the IndexWriter, opening one if needed.
	 *
	 * @param errorContextBuilder might contain some context useful to provide when handling IOExceptions.
	 * Is an optional parameter.
	 * @return a new IndexWriter or one already open.
	 */
	public IndexWriter getIndexWriter(ErrorContextBuilder errorContextBuilder) {
		IndexWriter indexWriter = writer.get();
		if ( indexWriter == null ) {
			writerInitializationLock.lock();
			try {
				indexWriter = writer.get();
				if ( indexWriter == null ) {
					try {
						indexWriter = createNewIndexWriter();
						log.trace( "IndexWriter opened" );
						writer.set( indexWriter );
					}
					catch (IOException ioe) {
						indexWriter = null;
						writer.set( null );
						handleIOException( ioe, errorContextBuilder );
					}
				}
			}
			finally {
				writerInitializationLock.unlock();
			}
		}
		return indexWriter;
	}

	public IndexWriter getIndexWriter() {
		return getIndexWriter( null );
	}

	/**
	 * Create as new IndexWriter using the passed in IndexWriterConfig as a template, but still applies some late changes:
	 * we need to override the MergeScheduler to handle background errors, and a new instance needs to be created for each
	 * new IndexWriter.
	 * Also each new IndexWriter needs a new MergePolicy.
	 */
	private IndexWriter createNewIndexWriter() throws IOException {
		final IndexWriterConfig indexWriterConfig = createWriterConfig(); //Each writer config can be attached only once to an IndexWriter
		/* TODO HSEARCH-3117 re-allow to configure index writers
		LogByteSizeMergePolicy newMergePolicy = indexParameters.getNewMergePolicy(); //TODO make it possible to configure a different policy?
		indexWriterConfig.setMergePolicy( newMergePolicy );
		 */
		MergeScheduler mergeScheduler = new HibernateSearchConcurrentMergeScheduler( this.errorHandler, this.indexName );
		indexWriterConfig.setMergeScheduler( mergeScheduler );
		return new IndexWriter( directory, indexWriterConfig );
	}

	private IndexWriterConfig createWriterConfig() {
		IndexWriterConfig writerConfig = new IndexWriterConfig( analyzer );
		/* TODO HSEARCH-3117 re-allow to configure index writers
		luceneParameters.applyToWriter( writerConfig );
		if ( similarity != null ) {
			writerConfig.setSimilarity( similarity );
		}
		 */
		writerConfig.setOpenMode( OpenMode.CREATE_OR_APPEND );
		return writerConfig;
	}

	/**
	 * Commits changes to a previously opened IndexWriter.
	 *
	 * @param errorContextBuilder use it to handle exceptions, as it might contain a reference to the work performed before the commit
	 */
	public void commitIndexWriter(ErrorContextBuilder errorContextBuilder) {
		IndexWriter indexWriter = writer.get();
		if ( indexWriter != null ) {
			try {
				indexWriter.commit();
				log.trace( "Index changes committed." );
			}
			catch (IOException ioe) {
				handleIOException( ioe, errorContextBuilder );
			}
		}
	}

	/**
	 * @see #commitIndexWriter(ErrorContextBuilder)
	 */
	public void commitIndexWriter() {
		commitIndexWriter( null );
	}

	/**
	 * Closes a previously opened IndexWriter.
	 */
	public void closeIndexWriter() {
		final IndexWriter toClose = writer.getAndSet( null );
		if ( toClose != null ) {
			try {
				toClose.close();
				log.trace( "IndexWriter closed" );
			}
			catch (IOException ioe) {
				forceLockRelease();
				handleIOException( ioe, null );
			}
		}
	}

	/**
	 * Forces release of Directory lock. Should be used only to cleanup as error recovery.
	 */
	public void forceLockRelease() {
		log.forcingReleaseIndexWriterLock();
		writerInitializationLock.lock();
		try {
			IndexWriter indexWriter = writer.getAndSet( null );
			if ( indexWriter != null ) {
				indexWriter.close();
				log.trace( "IndexWriter closed" );
			}
		}
		catch (IOException ioe) {
			handleIOException( ioe, null );
		}
		finally {
			writerInitializationLock.unlock();
		}
	}

	/**
	 * Opens an IndexReader having visibility on uncommitted writes from
	 * the IndexWriter, if any writer is open, or null if no IndexWriter is open.
	 * @param applyDeletes Applying deletes is expensive, say no if you can deal with stale hits during queries
	 * @return a new NRT IndexReader if an IndexWriter is available, or <code>null</code> otherwise
	 */
	public DirectoryReader openNRTIndexReader(boolean applyDeletes) {
		final IndexWriter indexWriter = writer.get();
		try {
			if ( indexWriter != null ) {
				// TODO HSEARCH-3117 should parameter writeAllDeletes take the same value as applyDeletes?
				return DirectoryReader.open( indexWriter, applyDeletes, applyDeletes );
			}
			else {
				return null;
			}
		}
		// following exceptions should be propagated as the IndexReader is needed by
		// the main thread
		catch (CorruptIndexException cie) {
			throw log.cantOpenCorruptedIndex( cie, indexName );
		}
		catch (IOException ioe) {
			throw log.ioExceptionOnIndex( ioe, indexName );
		}
	}

	/**
	 * Opens an IndexReader from the DirectoryProvider (not using the IndexWriter)
	 */
	public DirectoryReader openDirectoryIndexReader() {
		try {
			return DirectoryReader.open( directory );
		}
		// following exceptions should be propagated as the IndexReader is needed by
		// the main thread
		catch (CorruptIndexException cie) {
			throw log.cantOpenCorruptedIndex( cie, indexName );
		}
		catch (IOException ioe) {
			throw log.ioExceptionOnIndex( ioe, indexName );
		}
	}

	/**
	 * @param ioe The exception to handle
	 * @param errorContextBuilder Might be used to enqueue useful information about the lost operations, or be null
	 */
	private void handleIOException(IOException ioe, ErrorContextBuilder errorContextBuilder) {
		if ( log.isTraceEnabled() ) {
			log.trace( "going to handle IOException", ioe );
		}
		final ErrorContext errorContext;
		if ( errorContextBuilder != null ) {
			errorContext = errorContextBuilder.errorThatOccurred( ioe ).createErrorContext();
			this.errorHandler.handle( errorContext );
		}
		else {
			errorHandler.handleException( log.ioExceptionOnIndexWriter(), ioe );
		}
	}
}
