//$Id$
package org.hibernate.search.backend;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.slf4j.Logger;

import org.hibernate.annotations.common.AssertionFailure;
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
	 * To enter the optimization phase you need to acquire the lock first.
	 * @throws AssertionFailure if the lock is not owned.
	 */
	public void optimizerPhase() {
		assertOwnLock();
		// used getAndSet(0) because Workspace is going to be reused by next transaction.
		synchronized (optimizerStrategy) {
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
		// Needs to ensure the optimizerStrategy is accessed in threadsafe way
		synchronized (optimizerStrategy) {
			optimizerStrategy.optimizationForced();
		}
	}

	/**
	 * Gets the IndexWriter, opening one if needed.
	 * @param batchmode when true the indexWriter settings for batch mode will be applied.
	 * Ignored if IndexWriter is open already.
	 * @throws AssertionFailure if the lock is not owned.
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
	 * @throws SearchException on IOException during Lucene close operation.
	 * @throws AssertionFailure if there is no IndexWriter to close.
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
		else {
			throw new AssertionFailure( "No open IndexWriter to commit changes." );
		}
	}

	/**
	 * Closes a previously opened IndexWriter.
	 * @throws SearchException on IOException during Lucene close operation.
	 * @throws AssertionFailure if there is no IndexWriter to close.
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
		else {
			throw new AssertionFailure( "No open IndexWriter to close" );
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
	 * Acquires a lock on the DirectoryProvider backing this Workspace;
	 * this is required to use optimizerPhase()
	 * @see #optimizerPhase()
	 */
	public void lock() {
		lock.lock();
	}

	/**
	 * Releases the lock obtained by calling lock(). The caller must own the lock.
	 * @see #lock()
	 */
	public void unlock() {
		lock.unlock();
	}

	private void assertOwnLock() {
		if ( ! lock.isHeldByCurrentThread() )
			throw new AssertionFailure( "Not owning DirectoryProvider Lock" );
	}

}
