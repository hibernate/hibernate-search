/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.impl.lucene;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.LogByteSizeMergePolicy;
import org.apache.lucene.index.MergeScheduler;
import org.apache.lucene.search.similarities.Similarity;
import org.hibernate.search.backend.impl.lucene.analysis.ConcurrentlyMutableAnalyzer;
import org.hibernate.search.backend.impl.lucene.overrides.ConcurrentMergeScheduler;
import org.hibernate.search.backend.spi.LuceneIndexingParameters;
import org.hibernate.search.backend.spi.LuceneIndexingParameters.ParameterSet;
import org.hibernate.search.exception.ErrorContext;
import org.hibernate.search.exception.ErrorHandler;
import org.hibernate.search.exception.impl.ErrorContextBuilder;
import org.hibernate.search.indexes.spi.DirectoryBasedIndexManager;
import org.hibernate.search.store.DirectoryProvider;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * @author Sanne Grinovero (C) 2011 Red Hat Inc.
 */
class IndexWriterHolder {
	private static final Log log = LoggerFactory.make();

	private final ErrorHandler errorHandler;
	private final ParameterSet indexParameters;
	private final DirectoryProvider directoryProvider;
	private final String indexName;

	// variable state:

	/**
	 * Current open IndexWriter, or null when closed.
	 */
	private final AtomicReference<IndexWriter> writer = new AtomicReference<IndexWriter>();

	/**
	 * Protects from multiple initialization attempts of IndexWriter
	 */
	private final ReentrantLock writerInitializationLock = new ReentrantLock();

	/**
	 * The Similarity strategy needs to be applied on each new IndexWriter
	 */
	private final Similarity similarity;

	private final LuceneIndexingParameters luceneParameters;


	IndexWriterHolder(ErrorHandler errorHandler, DirectoryBasedIndexManager indexManager) {
		this.errorHandler = errorHandler;
		this.indexName = indexManager.getIndexName();
		this.luceneParameters = indexManager.getIndexingParameters();
		this.indexParameters = luceneParameters.getIndexParameters();
		this.directoryProvider = indexManager.getDirectoryProvider();
		this.similarity = indexManager.getSimilarity();
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
		LogByteSizeMergePolicy newMergePolicy = indexParameters.getNewMergePolicy(); //TODO make it possible to configure a different policy?
		indexWriterConfig.setMergePolicy( newMergePolicy );
		MergeScheduler mergeScheduler = new ConcurrentMergeScheduler( this.errorHandler, this.indexName );
		indexWriterConfig.setMergeScheduler( mergeScheduler );
		return new IndexWriter( directoryProvider.getDirectory(), indexWriterConfig );
	}

	private IndexWriterConfig createWriterConfig() {
		final ConcurrentlyMutableAnalyzer globalAnalyzer = new ConcurrentlyMutableAnalyzer( new SimpleAnalyzer() );
		final IndexWriterConfig writerConfig = new IndexWriterConfig( globalAnalyzer );
		luceneParameters.applyToWriter( writerConfig );
		if ( similarity != null ) {
			writerConfig.setSimilarity( similarity );
		}
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
				return DirectoryReader.open( indexWriter, applyDeletes );
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
			return DirectoryReader.open( directoryProvider.getDirectory() );
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
