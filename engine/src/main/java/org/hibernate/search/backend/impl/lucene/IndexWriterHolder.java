/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.search.backend.impl.lucene;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.LogByteSizeMergePolicy;
import org.apache.lucene.index.MergeScheduler;
import org.apache.lucene.search.Similarity;
import org.apache.lucene.store.Directory;
import org.hibernate.search.Environment;
import org.hibernate.search.backend.impl.lucene.overrides.ConcurrentMergeScheduler;
import org.hibernate.search.backend.spi.LuceneIndexingParameters;
import org.hibernate.search.backend.spi.LuceneIndexingParameters.ParameterSet;
import org.hibernate.search.exception.ErrorContext;
import org.hibernate.search.exception.ErrorHandler;
import org.hibernate.search.exception.impl.ErrorContextBuilder;
import org.hibernate.search.indexes.impl.DirectoryBasedIndexManager;
import org.hibernate.search.store.DirectoryProvider;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
class IndexWriterHolder {
	private static final Log log = LoggerFactory.make();

	/**
	 * This Analyzer is never used in practice: during Add operation it's overridden.
	 * So we don't care for the Version, using whatever Lucene thinks is safer.
	 */
	private static final Analyzer SIMPLE_ANALYZER = new SimpleAnalyzer( Environment.DEFAULT_LUCENE_MATCH_VERSION );

	private final IndexWriterConfig writerConfig = new IndexWriterConfig(
			Environment.DEFAULT_LUCENE_MATCH_VERSION,
			SIMPLE_ANALYZER
	);
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


	IndexWriterHolder(ErrorHandler errorHandler, DirectoryBasedIndexManager indexManager) {
		this.errorHandler = errorHandler;
		this.indexName = indexManager.getIndexName();
		LuceneIndexingParameters luceneParameters = indexManager.getIndexingParameters();
		this.indexParameters = luceneParameters.getIndexParameters();
		this.directoryProvider = indexManager.getDirectoryProvider();
		luceneParameters.applyToWriter( writerConfig );
		Similarity similarity = indexManager.getSimilarity();
		if ( similarity != null ) {
			writerConfig.setSimilarity( similarity );
		}
		writerConfig.setOpenMode( OpenMode.APPEND ); //More efficient to open
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
		LogByteSizeMergePolicy newMergePolicy = indexParameters.getNewMergePolicy(); //TODO make it possible to configure a different policy?
		writerConfig.setMergePolicy( newMergePolicy );
		MergeScheduler mergeScheduler = new ConcurrentMergeScheduler( this.errorHandler, this.indexName );
		writerConfig.setMergeScheduler( mergeScheduler );
		return new IndexWriter( directoryProvider.getDirectory(), writerConfig );
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
			try {
				IndexWriter indexWriter = writer.getAndSet( null );
				if ( indexWriter != null ) {
					indexWriter.close();
					log.trace( "IndexWriter closed" );
				}
			}
			finally {
				final Directory directory = directoryProvider.getDirectory();
				if ( IndexWriter.isLocked( directory ) ) {
					IndexWriter.unlock( directory );
				}
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
	public IndexReader openNRTIndexReader(boolean applyDeletes) {
		final IndexWriter indexWriter = writer.get();
		try {
			if ( indexWriter != null ) {
				return IndexReader.open( indexWriter, applyDeletes );
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
	public IndexReader openDirectoryIndexReader() {
		try {
			return IndexReader.open( directoryProvider.getDirectory() );
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
