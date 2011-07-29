/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search.backend;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.LogByteSizeMergePolicy;
import org.apache.lucene.index.MergeScheduler;
import org.apache.lucene.search.Similarity;
import org.apache.lucene.util.Version;

import org.hibernate.search.backend.spi.LuceneIndexingParameters;
import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;
import org.hibernate.search.util.logging.impl.Log;

import org.hibernate.search.SearchFactory;
import org.hibernate.search.backend.spi.LuceneIndexingParameters.ParameterSet;
import org.hibernate.search.backend.impl.lucene.overrides.ConcurrentMergeScheduler;
import org.hibernate.search.exception.ErrorContext;
import org.hibernate.search.exception.ErrorHandler;
import org.hibernate.search.exception.impl.ErrorContextBuilder;
import org.hibernate.search.exception.impl.SingleErrorContext;
import org.hibernate.search.indexes.impl.DirectoryBasedIndexManager;
import org.hibernate.search.store.DirectoryProvider;
import org.hibernate.search.store.optimization.OptimizerStrategy;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Lucene workspace for a DirectoryProvider.<p/>
 * Before using {@link #getIndexWriter} the lock must be acquired,
 * and resources must be closed before releasing the lock.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 * @author Sanne Grinovero
 */
//TODO renaming to "DirectoryWorkspace" would be nice.
//Clarify where it belongs SPI or Impl and what to expose to OptimizerStrategy
public class Workspace {

	private static final Log log = LoggerFactory.make();
	
	/**
	 * This Analyzer is never used in practice: during Add operation it's overriden.
	 * So we don't care for the Version, using whatever Lucene thinks is safer.
	 */
	private static final Analyzer SIMPLE_ANALYZER = new SimpleAnalyzer( Version.LUCENE_31 );
	
	// invariant state:

	private final DirectoryProvider<?> directoryProvider;
	private final OptimizerStrategy optimizerStrategy;
	private final Set<Class<?>> entitiesInDirectory;
	private final LuceneIndexingParameters indexingParams;
	private final ErrorHandler errorHandler;
	
	private final IndexWriterConfig writerConfig = new IndexWriterConfig( Version.LUCENE_31, SIMPLE_ANALYZER );

	// variable state:
	
	/**
	 * Current open IndexWriter, or null when closed. Guarded by synchronization.
	 */
	private IndexWriter writer;
	
	/**
	 * Keeps a count of modification operations done on the index.
	 */
	private final AtomicLong operations = new AtomicLong( 0L );

	private final DirectoryBasedIndexManager indexManager;
	
	public Workspace(DirectoryBasedIndexManager indexManager, ErrorHandler errorHandler) {
		this.indexManager = indexManager;
		this.directoryProvider = indexManager.getDirectoryProvider();
		this.optimizerStrategy = indexManager.getOptimizerStrategy();
		this.entitiesInDirectory = indexManager.getContainedTypes();
		this.indexingParams = indexManager.getIndexingParameters();
		this.errorHandler = errorHandler;
		indexingParams.applyToWriter( writerConfig );
		Similarity similarity = indexManager.getSimilarity();
		if ( similarity != null ) {
			writerConfig.setSimilarity( similarity );
		}
		indexManager.setIndexWriterConfig( writerConfig );
	}

	public <T> DocumentBuilderIndexedEntity<?> getDocumentBuilder(Class<T> entity) {
		return indexManager.getIndexBindingForEntity( entity ).getDocumentBuilder();
	}

	public Analyzer getAnalyzer(String name) {
		return indexManager.getAnalyzer( name );
	}

	/**
	 * If optimization has not been forced give a chance to configured OptimizerStrategy
	 * to optimize the index.
	 */
	public void optimizerPhase() {
		// used getAndSet(0) because Workspace is going to be reused by next transaction.
		synchronized ( optimizerStrategy ) {
			optimizerStrategy.addTransaction( operations.getAndSet( 0L ) );
			optimizerStrategy.optimize( this );
		}
	}
	
	/**
	 * Used by OptimizeLuceneWork after index optimization to flag that
	 * optimization has been forced.
	 * @see OptimizeLuceneWork
	 * @see SearchFactory#optimize()
	 * @see SearchFactory#optimize(Class)
	 */
	public void optimize() {
		//Needs to ensure the optimizerStrategy is accessed in threadsafe way
		synchronized ( optimizerStrategy ) {
			optimizerStrategy.optimizationForced();
		}
	}

	/**
	 * Gets the IndexWriter, opening one if needed.
	 * @param errorContextBuilder might contain some context useful to provide when handling IOExceptions
	 * @return a new IndexWriter or one already open.
	 */
	public synchronized IndexWriter getIndexWriter(ErrorContextBuilder errorContextBuilder) {
		if ( writer != null )
			return writer;
		try {
			ParameterSet indexingParameters = indexingParams.getIndexParameters();
			writer = createNewIndexWriter( directoryProvider, this.writerConfig, indexingParameters );
			log.trace( "IndexWriter opened" );
		}
		catch ( IOException ioe ) {
			writer = null;
			handleIOException( ioe, errorContextBuilder );
		}
		return writer;
	}
	
	/**
	 * Create as new IndexWriter using the passed in IndexWriterConfig as a template, but still applies some late changes:
	 * we need to override the MergeScheduler to handle background errors, and a new instance needs to be created for each
	 * new IndexWriter.
	 * Also each new IndexWriter needs a new MergePolicy.
	 */
	private IndexWriter createNewIndexWriter(DirectoryProvider<?> directoryProvider, IndexWriterConfig writerConfig, ParameterSet indexingParameters) throws IOException {
		LogByteSizeMergePolicy newMergePolicy = indexingParameters.getNewMergePolicy(); //TODO make it possible to configure a different policy?
		writerConfig.setMergePolicy( newMergePolicy );
		MergeScheduler mergeScheduler = new ConcurrentMergeScheduler( this.errorHandler );
		writerConfig.setMergeScheduler( mergeScheduler );
		IndexWriter writer = new IndexWriter( directoryProvider.getDirectory(), writerConfig );
		return writer;
	}

	/**
	 * @see #getIndexWriter(ErrorContextBuilder)
	 */
	public IndexWriter getIndexWriter() {
		return getIndexWriter( null );
	}

	/**
	 * Commits changes to a previously opened IndexWriter.
	 * @param errorContextBuilder use it to handle exceptions, as it might contain a reference to the work performed before the commit
	 */
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
	public synchronized void commitIndexWriter() {
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
				handleIOException( ioe, null );
			}
		}
	}

	/**
	 * Increment the counter of modification operations done on the index.
	 * Used (currently only) by the OptimizerStrategy.
	 * @param modCount the increment to add to the counter.
	 */
	public void incrementModificationCounter(int modCount) {
		operations.addAndGet( modCount );
	}

	/**
	 * @return The unmodifiable set of entity types being indexed
	 * in the underlying Lucene Directory backing this Workspace.
	 */
	public Set<Class<?>> getEntitiesInDirectory() {
		return entitiesInDirectory;
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
		}
		else {
			 errorContext = new SingleErrorContext( ioe );
		}
		this.errorHandler.handle( errorContext );
	}

}
