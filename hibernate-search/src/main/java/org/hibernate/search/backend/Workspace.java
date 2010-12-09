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
import java.util.concurrent.locks.ReentrantLock;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.MergeScheduler;
import org.apache.lucene.search.Similarity;
import org.slf4j.Logger;

import org.hibernate.search.spi.WorkerBuildContext;
import org.hibernate.search.SearchFactory;
import org.hibernate.search.backend.impl.lucene.overrides.ConcurrentMergeScheduler;
import org.hibernate.search.engine.DocumentBuilderIndexedEntity;
import org.hibernate.search.engine.SearchFactoryImplementor;
import org.hibernate.search.exception.ErrorContext;
import org.hibernate.search.exception.ErrorHandler;
import org.hibernate.search.exception.impl.ErrorContextBuilder;
import org.hibernate.search.exception.impl.SingleErrorContext;
import org.hibernate.search.store.DirectoryProvider;
import org.hibernate.search.store.optimization.OptimizerStrategy;
import org.hibernate.search.util.LoggerFactory;

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
public class Workspace {

	private static final Logger log = LoggerFactory.make();
	private static final Analyzer SIMPLE_ANALYZER = new SimpleAnalyzer();
	private static final IndexWriter.MaxFieldLength maxFieldLength =
		new IndexWriter.MaxFieldLength( IndexWriter.DEFAULT_MAX_FIELD_LENGTH );
	
	// invariant state:

	private final SearchFactoryImplementor searchFactoryImplementor;
	private final DirectoryProvider<?> directoryProvider;
	private final OptimizerStrategy optimizerStrategy;
	private final ReentrantLock lock;
	private final Set<Class<?>> entitiesInDirectory;
	private final LuceneIndexingParameters indexingParams;
	private final Similarity similarity;
	private final ErrorHandler errorHandler;

	// variable state:
	
	/**
	 * Current open IndexWriter, or null when closed. Guarded by synchronization.
	 */
	private IndexWriter writer;
	
	/**
	 * Keeps a count of modification operations done on the index.
	 */
	private final AtomicLong operations = new AtomicLong( 0L );
	
	public Workspace(WorkerBuildContext context, DirectoryProvider<?> provider, ErrorHandler errorHandler) {
		this.searchFactoryImplementor = context.getUninitializedSearchFactory();
		this.directoryProvider = provider;
		this.optimizerStrategy = context.getOptimizerStrategy( directoryProvider );
		this.entitiesInDirectory = context.getClassesInDirectoryProvider( provider );
		this.indexingParams = context.getIndexingParameters( directoryProvider );
		this.lock = context.getDirectoryProviderLock( provider );
		this.similarity = context.getSimilarity( directoryProvider );
		this.errorHandler = errorHandler;
	}

	public <T> DocumentBuilderIndexedEntity<T> getDocumentBuilder(Class<T> entity) {
		return searchFactoryImplementor.getDocumentBuilderIndexedEntity( entity );
	}

	public Analyzer getAnalyzer(String name) {
		return searchFactoryImplementor.getAnalyzer( name );
	}

	/**
	 * If optimization has not been forced give a chance to configured OptimizerStrategy
	 * to optimize the index.
	 */
	public void optimizerPhase() {
		lock.lock();
		try {
			// used getAndSet(0) because Workspace is going to be reused by next transaction.
			synchronized ( optimizerStrategy ) {
				optimizerStrategy.addTransaction( operations.getAndSet( 0L ) );
				optimizerStrategy.optimize( this );
			}
		}
		finally {
			lock.unlock();
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
		lock.lock();
		try {
			//Needs to ensure the optimizerStrategy is accessed in threadsafe way
			synchronized ( optimizerStrategy ) {
				optimizerStrategy.optimizationForced();
			}
		}
		finally {
			lock.unlock();
		}
	}

	/**
	 * Gets the IndexWriter, opening one if needed.
	 * @param batchmode when true the indexWriter settings for batch mode will be applied.
	 * Ignored if IndexWriter is open already.
	 * @param builder might contain some context useful to provide when handling IOExceptions
	 * @return a new IndexWriter or one already open.
	 */
	public synchronized IndexWriter getIndexWriter(boolean batchmode, ErrorContextBuilder builder) {
		if ( writer != null )
			return writer;
		try {
			writer = new IndexWriter( directoryProvider.getDirectory(), SIMPLE_ANALYZER, false, maxFieldLength ); // has been created at init time
			indexingParams.applyToWriter( writer, batchmode );
			writer.setSimilarity( similarity );
			MergeScheduler mergeScheduler = new ConcurrentMergeScheduler( this.errorHandler );
			writer.setMergeScheduler( mergeScheduler );
			log.trace( "IndexWriter opened" );
		}
		catch ( IOException ioe ) {
			writer = null;
			handleIOException( ioe, builder );
		}
		return writer;
	}
	
	/**
	 * @see #getIndexWriter(boolean, ErrorContextBuilder)
	 */
	public IndexWriter getIndexWriter(boolean batchmode) {
		return getIndexWriter( batchmode, null );
	}

	/**
	 * Commits changes to a previously opened IndexWriter.
	 * @param builder use it to handle exceptions, as it might contain a reference to the work performed before the commit
	 */
	public synchronized void commitIndexWriter(ErrorContextBuilder builder) {
		if ( writer != null ) {
			try {
				writer.commit();
				log.trace( "Index changes commited." );
			}
			catch ( IOException ioe ) {
				handleIOException( ioe, builder );
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
		log.warn( "going to force release of the IndexWriter lock" );
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
	 * @param errorBuilder Might be used to enqueue useful information about the lost operations, or be null
	 */
	private void handleIOException(IOException ioe, ErrorContextBuilder errorBuilder) {
		if ( log.isTraceEnabled() ) {
			log.trace( "going to handle IOException", ioe );
		}
		final ErrorContext errorContext;
		if ( errorBuilder != null ) {
			errorContext = errorBuilder.errorThatOccurred( ioe ).createErrorContext();
		}
		else {
			 errorContext = new SingleErrorContext( ioe );
		}
		this.errorHandler.handle( errorContext );
	}

}
