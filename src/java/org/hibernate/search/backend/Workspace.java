//$Id$
package org.hibernate.search.backend;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.Directory;
import org.hibernate.annotations.common.AssertionFailure;
import org.hibernate.search.SearchException;
import org.hibernate.search.engine.DocumentBuilder;
import org.hibernate.search.engine.SearchFactoryImplementor;
import org.hibernate.search.store.DirectoryProvider;
import org.hibernate.search.store.optimization.OptimizerStrategy;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * Lucene workspace.
 * <p/>
 * <b>This is not intended to be used in a multithreaded environment</b>.
 * <p/>
 * <ul>
 * <li>One cannot execute modification through an IndexReader when an IndexWriter has been acquired
 * on the same underlying directory
 * </li>
 * <li>One cannot get an IndexWriter when an IndexReader have been acquired and modified on the same
 * underlying directory
 * </li>
 * <li>The recommended approach is to execute all the modifications on the IndexReaders, {@link #clean()}, and acquire the
 * index writers
 * </li>
 * </ul>
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 * @author Sanne Grinovero
 */
//TODO introduce the notion of read only IndexReader? We cannot enforce it because Lucene uses abstract classes, not interfaces.
public class Workspace {
	
	private final Logger log = LoggerFactory.getLogger( Workspace.class );
	private final SearchFactoryImplementor searchFactoryImplementor;
	private static final Analyzer SIMPLE_ANALYZER = new SimpleAnalyzer();
	private final Map<DirectoryProvider, DPWorkspace> directorySpace = new HashMap<DirectoryProvider, DPWorkspace>() {

		@Override
		public DPWorkspace get(Object key) {
			DirectoryProvider dp = (DirectoryProvider) key;
			DPWorkspace directoryWorkSpace = super.get( dp );
			if ( directoryWorkSpace==null ) {
				directoryWorkSpace = new DPWorkspace( dp );
				put( dp, directoryWorkSpace );
			}
			return directoryWorkSpace;
		}
		
	};
	
	/**
	 * Flag indicating if the current work should be executed using
	 * the Lucene parameters for batch indexing.
	 */
	private boolean isBatch = false;

	public Workspace(SearchFactoryImplementor searchFactoryImplementor) {
		this.searchFactoryImplementor = searchFactoryImplementor;
	}

	public DocumentBuilder getDocumentBuilder(Class entity) {
		return searchFactoryImplementor.getDocumentBuilders().get( entity );
	}

	/**
	 * Retrieve a read write IndexReader; the purpose should be to
	 * modify the index. (mod count will be incremented)
	 * For a given DirectoryProvider, An IndexReader must be used before an IndexWriter
	 */
	public IndexReader getIndexReader(DirectoryProvider provider, Class entity) {
		DPWorkspace space = directorySpace.get( provider );
		return space.getIndexReader( entity );
	}

	//for index optimization
	public IndexWriter getIndexWriter(DirectoryProvider provider) {
		return getIndexWriter( provider, null, false );
	}

	/**
	 * retrieve a read write IndexWriter
	 * For a given DirectoryProvider, An IndexReader must be used before an IndexWriter
	 */
	public IndexWriter getIndexWriter(DirectoryProvider provider, Class entity, boolean modificationOperation) {
		DPWorkspace space = directorySpace.get( provider );
		return space.getIndexWriter( entity, modificationOperation );
	}

	private void cleanUp(SearchException originalException) {
		//release all readers and writers, then release locks
		SearchException raisedException = originalException;
		for ( DPWorkspace space : directorySpace.values() ) {
			try {
				space.closeIndexReader();
			}
			catch (IOException e) {
				if ( raisedException != null ) {
					log.error( "Subsequent Exception while closing IndexReader", e );
				}
				else {
					raisedException = new SearchException( "Exception while closing IndexReader", e );
				}
			}
		}
		//first release all locks for DirectoryProviders not needing optimization
		for ( DPWorkspace space : directorySpace.values() ) {
			if ( ! space.needsOptimization() ) {
				raisedException = closeWriterAndUnlock( space, raisedException );
			}
		}
		//then for remaining DirectoryProvider
		for ( DPWorkspace space : directorySpace.values() ) {
			if ( space.needsOptimization() ) {
				if ( raisedException == null ) {//avoid optimizations in case of errors or exceptions
					OptimizerStrategy optimizerStrategy = space.getOptimizerStrategy();
					optimizerStrategy.addTransaction( space.countOperations() );
					try {
						optimizerStrategy.optimize( this );
					}
					catch (SearchException e) {
						//this will also cause skipping other optimizations:
						raisedException = new SearchException( "Exception while optimizing directoryProvider: "
								+ space.getDirectory().toString(), e );
					}
				}
				raisedException = closeWriterAndUnlock( space, raisedException );
			}
		}
		if ( raisedException != null ) throw raisedException;
	}

	private SearchException closeWriterAndUnlock( DPWorkspace space, SearchException raisedException ) {
		try {
			space.closeIndexWriter();
		}
		catch (IOException e) {
			if ( raisedException != null ) {
				log.error( "Subsequent Exception while closing IndexWriter", e );
			}
			else {
				raisedException = new SearchException( "Exception while closing IndexWriter", e );
			}
		}
		space.unLock();
		return raisedException;
	}

	/**
	 * release resources consumed in the workspace if any
	 */
	public void clean() {
		cleanUp( null );
	}

	public void optimize(DirectoryProvider provider) {
		DPWorkspace space = directorySpace.get( provider );
		OptimizerStrategy optimizerStrategy = space.getOptimizerStrategy();
		space.setOptimizationForced();
		optimizerStrategy.optimizationForced();
	}

	public boolean isBatch() {
		return isBatch;
	}

	public void setBatch(boolean isBatch) {
		this.isBatch = isBatch;
	}
	
	//TODO this should have it's own source file but currently needs the container-Workspace cleanup()
	//for exceptions. Best to wait for move to parallel batchWorkers (one per Directory)?
	/**
	 * A per-DirectoryProvider Workspace
	 */
	private class DPWorkspace {
		
		private final DirectoryProvider directoryProvider;
		private final ReentrantLock lock;
		
		private IndexReader reader;
		private IndexWriter writer;
		private boolean locked = false;
		private boolean optimizationForced = false;
		private long operations = 0L;
		
		DPWorkspace(DirectoryProvider dp) {
			this.directoryProvider = dp;
			this.lock = searchFactoryImplementor.getLockableDirectoryProviders().get( dp );
		}
		
		public boolean needsOptimization() {
			return isLocked() && !isOptimizationForced();
		}

		/**
		 * Sets a flag to signal optimization has been forced.
		 * Cannot be undone.
		 */
		void setOptimizationForced() {
			optimizationForced = true;			
		}

		/**
		 * @return the Directory from the DirectoryProvider
		 */
		Directory getDirectory() {
			return directoryProvider.getDirectory();
		}

		/**
		 * @return A count of performed modification operations
		 */
		long countOperations() {
			return operations;
		}

		/**
		 * @return The OptimizerStrategy configured for this DirectoryProvider
		 */
		OptimizerStrategy getOptimizerStrategy() {
			return searchFactoryImplementor.getOptimizerStrategy( directoryProvider );
		}

		/**
		 * @return true if optimization has been forced
		 */
		boolean isOptimizationForced() {
			return optimizationForced;
		}

		/**
		 * @return true if underlying DirectoryProvider is locked
		 */
		boolean isLocked() {
			return locked;
		}

		/**
		 * Gets the currently open IndexWriter, or creates one.
		 * If a IndexReader was open, it will be closed first.
		 * @param entity The entity for which the IndexWriter is needed
		 * @param modificationOperation set to true if needed to perform modifications to index
		 * @return created or existing IndexWriter
		 */
		IndexWriter getIndexWriter(Class entity, boolean modificationOperation) {
			//one has to close a reader for update before a writer is accessed
			try {
				closeIndexReader();
			}
			catch (IOException e) {
				throw new SearchException( "Exception while closing IndexReader", e );
			}
			if ( modificationOperation ) {
				operations++; //estimate the number of modifications done on this index
			}
			if ( writer != null ) return writer;
			lock();
			try {
				DocumentBuilder documentBuilder = searchFactoryImplementor.getDocumentBuilders().get( entity );
				Analyzer analyzer = entity != null ?
						documentBuilder.getAnalyzer() :
						SIMPLE_ANALYZER; //never used
				writer = new IndexWriter( directoryProvider.getDirectory(), analyzer, false ); //has been created at init time
				writer.setSimilarity( documentBuilder.getSimilarity() );
				LuceneIndexingParameters indexingParams = searchFactoryImplementor.getIndexingParameters( directoryProvider );
				indexingParams.applyToWriter( writer, isBatch );
			}
			catch (IOException e) {
				cleanUp(
						new SearchException( "Unable to open IndexWriter" + ( entity != null ? " for " + entity : "" ), e )
				);
			}
			return writer;
		}

		/**
		 * Gets an IndexReader to alter the index;
		 * (operations count will be incremented)
		 * @throws AssertionFailure if an IndexWriter is open
		 * @param entity The entity for which the IndexWriter is needed
		 * @return a new or existing IndexReader
		 */
		IndexReader getIndexReader(Class entity) {
			//one cannot access a reader for update after a writer has been accessed
			if ( writer != null )
				throw new AssertionFailure( "Tries to read for update an index while a writer is accessed for " + entity );
			operations++; //estimate the number of modifications done on this index
			if ( reader != null ) return reader;
			lock();
			try {
				reader = IndexReader.open( directoryProvider.getDirectory() );
			}
			catch (IOException e) {
				cleanUp( new SearchException( "Unable to open IndexReader for " + entity, e ) );
			}
			return reader;
		}

		/**
		 * Unlocks underlying DirectoryProvider iff locked.
		 */
		void unLock() {
			if ( locked ) {
				lock.unlock();
				locked = false;
			}
		}
		
		/**
		 * Locks underlying DirectoryProvider iff not locked already.
		 */
		void lock() {
			if ( !locked ) {
				lock.lock();
				locked = true;
			}
		}

		/**
		 * Closes the IndexReader, if open.
		 * @throws IOException
		 */
		void closeIndexReader() throws IOException {
			IndexReader toClose = reader;
			reader = null;
			if ( toClose!=null ) {
				toClose.close();
			}
		}
		
		/**
		 * Closes the IndexWriter, if open.
		 * @throws IOException
		 */
		void closeIndexWriter() throws IOException {
			IndexWriter toClose = writer;
			writer = null;
			if ( toClose!=null ) {
				toClose.close();
			}
		}
		
	}
	
}
