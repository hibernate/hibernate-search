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

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.LogByteSizeMergePolicy;
import org.apache.lucene.index.MergeScheduler;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.search.Similarity;

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
	
	private final IndexWriterConfig writerConfig = new IndexWriterConfig( Environment.DEFAULT_LUCENE_MATCH_VERSION , SIMPLE_ANALYZER );
	private final LuceneIndexingParameters luceneParameters;
	private final ErrorHandler errorHandler;
	private final ParameterSet indexParameters;
	private final DirectoryProvider directoryProvider;
	private final String indexName;
	
	// variable state:
	
	/**
	 * Current open IndexWriter, or null when closed. Guarded by synchronization.
	 */
	private IndexWriter writer;


	IndexWriterHolder(ErrorHandler errorHandler, DirectoryBasedIndexManager indexManager) {
		this.errorHandler = errorHandler;
		this.indexName = indexManager.getIndexName();
		this.luceneParameters = indexManager.getIndexingParameters();
		this.indexParameters = luceneParameters.getIndexParameters();
		this.directoryProvider = indexManager.getDirectoryProvider();
		this.luceneParameters.applyToWriter( writerConfig );
		Similarity similarity = indexManager.getSimilarity();
		if ( similarity != null ) {
			writerConfig.setSimilarity( similarity );
		}
		writerConfig.setOpenMode( OpenMode.APPEND ); //More efficient to open
		//TODO remove this awful need to set a reference back again to the indexManager:
		indexManager.setIndexWriterConfig( writerConfig );
	}

	/**
	 * Gets the IndexWriter, opening one if needed.
	 * @param errorContextBuilder might contain some context useful to provide when handling IOExceptions.
	 *  Is an optional parameter.
	 * @return a new IndexWriter or one already open.
	 */
	public synchronized IndexWriter getIndexWriter(ErrorContextBuilder errorContextBuilder) {
		if ( writer != null )
			return writer;
		try {
			writer = createNewIndexWriter();
			log.trace( "IndexWriter opened" );
		}
		catch ( IOException ioe ) {
			writer = null;
			handleIOException( ioe, errorContextBuilder );
		}
		return writer;
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
		IndexWriter writer = new IndexWriter( directoryProvider.getDirectory(), writerConfig );
		return writer;
	}

	/**
	 * Commits changes to a previously opened IndexWriter.
	 * @param errorContextBuilder use it to handle exceptions, as it might contain a reference to the work performed before the commit
	 */
	//TODO HSEARCH-852 : a commit should not block a getIndexWriter: split the locking
	public synchronized void commitIndexWriter(ErrorContextBuilder errorContextBuilder) {
		if ( writer != null ) {
			try {
				writer.commit();
				log.trace( "Index changes commited." );
			}
			catch ( IOException ioe ) {
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
	public synchronized void closeIndexWriter() {
		IndexWriter toClose = writer;
		writer = null;
		if ( toClose != null ) {
			try {
				toClose.close();
				log.trace( "IndexWriter closed" );
			}
			catch ( IOException ioe ) {
				forceLockRelease();
				handleIOException( ioe, null );
			}
		}
	}

	/**
	 * Forces release of Directory lock. Should be used only to cleanup as error recovery.
	 */
	public synchronized void forceLockRelease() {
		log.forcingReleaseIndexWriterLock();
		try {
			try {
				if ( writer != null ) {
					writer.close();
					log.trace( "IndexWriter closed" );
				}
			}
			finally {
				writer = null; //make sure to send a faulty writer into garbage
				IndexWriter.unlock( directoryProvider.getDirectory() );
			}
		}
		catch (IOException ioe) {
			handleIOException( ioe, null );
		}
	}

	/**
	 * Opens an IndexReader having visibility on uncommitted writes from
	 * the IndexWriter, if any writer is open, or null if no IndexWriter is open.
	 */
	//TODO HSEARCH-852 : fine grained synchronization
	public synchronized IndexReader openNRTIndexReader(boolean applyDeletes) {
		try {
			if ( writer != null ) {
				return IndexReader.open( writer, applyDeletes );
			}
			else {
				return null;
			}
		}
		// following exceptions should be propagated as the IndexReader is needed by
		// the main thread
		catch ( CorruptIndexException cie ) {
			throw log.cantOpenCorruptedIndex( cie, indexName );
		}
		catch ( IOException ioe ) {
			throw log.ioExceptionOnIndex( ioe, indexName );
		}
	}

	/**
	 * Opens an IndexReader from the DirectoryProvider (not using the IndexWriter)
	 */
	public IndexReader openDirectoryIndexReader() {
		try {
			return IndexReader.open( directoryProvider.getDirectory(), true );
		}
		// following exceptions should be propagated as the IndexReader is needed by
		// the main thread
		catch ( CorruptIndexException cie ) {
			throw log.cantOpenCorruptedIndex( cie, indexName );
		}
		catch ( IOException ioe ) {
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
			errorHandler.handleException( log.ioExceptionOnIndexWriter() , ioe );
		}
	}

}
