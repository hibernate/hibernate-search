/* $Id$
 * 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * Copyright (c) 2009, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
import org.slf4j.Logger;

import org.hibernate.search.SearchException;
import org.hibernate.search.SearchFactory;
import org.hibernate.search.engine.DocumentBuilderIndexedEntity;
import org.hibernate.search.engine.SearchFactoryImplementor;
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

	// variable state:
	
	/**
	 * Current open IndexWriter, or null when closed. Guarded by synchronization.
	 */
	private IndexWriter writer;
	
	/**
	 * Keeps a count of modification operations done on the index.
	 */
	private final AtomicLong operations = new AtomicLong( 0L );
	
	public Workspace(SearchFactoryImplementor searchFactoryImplementor, DirectoryProvider<?> provider) {
		this.searchFactoryImplementor = searchFactoryImplementor;
		this.directoryProvider = provider;
		this.optimizerStrategy = searchFactoryImplementor.getOptimizerStrategy( directoryProvider );
		this.entitiesInDirectory = searchFactoryImplementor.getClassesInDirectoryProvider( provider );
		this.indexingParams = searchFactoryImplementor.getIndexingParameters( directoryProvider );
		this.lock = searchFactoryImplementor.getDirectoryProviderLock( provider );
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
			synchronized (optimizerStrategy) {
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
	 * @throws SearchException on a IOException during index opening.
	 * @return a new IndexWriter or one already open.
	 */
	public synchronized IndexWriter getIndexWriter(boolean batchmode) {
		if ( writer != null )
			return writer;
		try {
			writer = new IndexWriter( directoryProvider.getDirectory(), SIMPLE_ANALYZER, false, maxFieldLength ); // has been created at init time
			indexingParams.applyToWriter( writer, batchmode );
			log.trace( "IndexWriter opened" );
		}
		catch ( IOException e ) {
			writer = null;
			throw new SearchException( "Unable to open IndexWriter", e );
		}
		return writer;
	}

	/**
	 * Commits changes to a previously opened IndexWriter.
	 *
	 * @throws SearchException on IOException during Lucene close operation,
	 * or if there is no IndexWriter to close.
	 */
	public synchronized void commitIndexWriter() {
		if ( writer != null ) {
			try {
				writer.commit();
				log.trace( "Index changes commited." );
			}
			catch ( IOException e ) {
				throw new SearchException( "Exception while commiting index changes", e );
			}
		}
	}

	/**
	 * Closes a previously opened IndexWriter.
	 * @throws SearchException on IOException during Lucene close operation
	 */
	public synchronized void closeIndexWriter() {
		IndexWriter toClose = writer;
		writer = null;
		if ( toClose != null ) {
			try {
				toClose.close();
				log.trace( "IndexWriter closed" );
			}
			catch ( IOException e ) {
				throw new SearchException( "Exception while closing IndexWriter", e );
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

}
