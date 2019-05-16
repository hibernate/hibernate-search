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
import org.hibernate.search.engine.common.spi.ErrorHandler;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.MergeScheduler;
import org.apache.lucene.store.Directory;

/**
 * @author Sanne Grinovero (C) 2011 Red Hat Inc.
 */
public class IndexWriterHolder {
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

	public IndexWriterHolder(String indexName, Directory directory, Analyzer analyzer, ErrorHandler errorHandler) {
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
	 * @return a new IndexWriter or one already open.
	 */
	public IndexWriter getIndexWriter() throws IOException {
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
	 * Closes a previously opened IndexWriter.
	 */
	public void closeIndexWriter() throws IOException {
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

	/**
	 * Forces release of Directory lock. Should be used only to cleanup as error recovery.
	 */
	public void forceLockRelease() throws IOException {
		log.forcingReleaseIndexWriterLock();
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

	/**
	 * Opens an IndexReader having visibility on uncommitted writes from
	 * the IndexWriter, if any writer is open, or null if no IndexWriter is open.
	 * @param applyDeletes Applying deletes is expensive, say no if you can deal with stale hits during queries
	 * @return a new NRT IndexReader if an IndexWriter is available, or <code>null</code> otherwise
	 */
	public DirectoryReader openNRTIndexReader(boolean applyDeletes) throws IOException {
		final IndexWriter indexWriter = writer.get();
		if ( indexWriter != null ) {
			// TODO HSEARCH-3117 should parameter writeAllDeletes take the same value as applyDeletes?
			return DirectoryReader.open( indexWriter, applyDeletes, applyDeletes );
		}
		else {
			return null;
		}
	}

	/**
	 * Opens an IndexReader from the DirectoryProvider (not using the IndexWriter)
	 */
	public DirectoryReader openDirectoryIndexReader() throws IOException {
		return DirectoryReader.open( directory );
	}

}
