//$Id$
package org.hibernate.search.backend;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.hibernate.annotations.common.AssertionFailure;
import org.hibernate.search.SearchException;
import org.hibernate.search.engine.DocumentBuilder;
import org.hibernate.search.engine.SearchFactoryImplementor;
import org.hibernate.search.store.DirectoryProvider;
import org.hibernate.search.store.optimization.OptimizerStrategy;

/**
 * Lucene workspace.
 * <p/>
 * <b>This is not intended to be used in a multithreaded environment</b>.
 * <p/>
 * <ul>
 * <li>One cannot execute modification through an IndexReader when an IndexWriter has been acquired
 * on the same underlying directory
 * </li>
 * <li>One cannot get an IndexWriter when an IndexReader have been acquired and modificed on the same
 * underlying directory
 * </li>
 * <li>The recommended approach is to execute all the modifications on the IndexReaders, {@link #clean()}, and acquire the
 * index writers
 * </li>
 * </ul>
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
//TODO introduce the notion of read only IndexReader? We cannot enforce it because Lucene use abstract classes, not interfaces
public class Workspace {
	private static Log log = LogFactory.getLog( Workspace.class );
	private Map<DirectoryProvider, IndexReader> readers = new HashMap<DirectoryProvider, IndexReader>();
	private Map<DirectoryProvider, IndexWriter> writers = new HashMap<DirectoryProvider, IndexWriter>();
	private List<DirectoryProvider> lockedProviders = new ArrayList<DirectoryProvider>();
	private Map<DirectoryProvider, DPStatistics> dpStatistics = new HashMap<DirectoryProvider, DPStatistics>();
	private SearchFactoryImplementor searchFactoryImplementor;

	/**
	 * Flag indicating if the current work should be executed the Lucene parameters for batch indexing.
	 */
	private boolean isBatch = false;


	public Workspace(SearchFactoryImplementor searchFactoryImplementor) {
		this.searchFactoryImplementor = searchFactoryImplementor;
	}

	public DocumentBuilder getDocumentBuilder(Class entity) {
		return searchFactoryImplementor.getDocumentBuilders().get( entity );
	}

	/**
	 * retrieve a read write IndexReader
	 * For a given DirectoryProvider, An IndexReader must be used before an IndexWriter
	 */
	public IndexReader getIndexReader(DirectoryProvider provider, Class entity) {
		//one cannot access a reader for update after a writer has been accessed
		if ( writers.containsKey( provider ) )
			throw new AssertionFailure( "Tries to read for update an index while a writer is accessed" + entity );
		IndexReader reader = readers.get( provider );
		if ( reader != null ) return reader;
		lockProvider( provider );
		dpStatistics.get( provider ).operations++;
		try {
			reader = IndexReader.open( provider.getDirectory() );
			readers.put( provider, reader );
		}
		catch (IOException e) {
			cleanUp( new SearchException( "Unable to open IndexReader for " + entity, e ) );
		}
		return reader;
	}

	//for optimization
	public IndexWriter getIndexWriter(DirectoryProvider provider) {
		return getIndexWriter( provider, null, false );
	}

	/**
	 * retrieve a read write IndexWriter
	 * For a given DirectoryProvider, An IndexReader must be used before an IndexWriter
	 */
	public IndexWriter getIndexWriter(DirectoryProvider provider, Class entity, boolean modificationOperation) {
		//one has to close a reader for update before a writer is accessed
		IndexReader reader = readers.get( provider );
		if ( reader != null ) {
			try {
				reader.close();
			}
			catch (IOException e) {
				throw new SearchException( "Exception while closing IndexReader", e );
			}
			readers.remove( provider );
		}
		IndexWriter writer = writers.get( provider );
		if ( writer != null ) return writer;
		lockProvider( provider );
		if ( modificationOperation ) dpStatistics.get( provider ).operations++;
		try {
			Analyzer analyzer = entity != null ?
					searchFactoryImplementor.getDocumentBuilders().get( entity ).getAnalyzer() :
					new SimpleAnalyzer(); //never used
			writer = new IndexWriter( provider.getDirectory(), analyzer, false ); //has been created at init time

			LuceneIndexingParameters indexingParams = searchFactoryImplementor.getIndexingParameters( provider );
			if ( isBatch ) {
				writer.setMergeFactor( indexingParams.getBatchMergeFactor() );
				writer.setMaxMergeDocs( indexingParams.getBatchMaxMergeDocs() );
				writer.setMaxBufferedDocs( indexingParams.getBatchMaxBufferedDocs() );
			}
			else {
				writer.setMergeFactor( indexingParams.getTransactionMergeFactor() );
				writer.setMaxMergeDocs( indexingParams.getTransactionMaxMergeDocs() );
				writer.setMaxBufferedDocs( indexingParams.getTransactionMaxBufferedDocs() );
			}

			writers.put( provider, writer );
		}
		catch (IOException e) {
			cleanUp(
					new SearchException( "Unable to open IndexWriter" + ( entity != null ? " for " + entity : "" ), e )
			);
		}
		return writer;
	}

	private void lockProvider(DirectoryProvider provider) {
		//make sure to use a semaphore
		ReentrantLock lock = searchFactoryImplementor.getLockableDirectoryProviders().get( provider );
		//of course a given thread cannot have a race cond with itself
		if ( !lock.isHeldByCurrentThread() ) {
			lock.lock();
			lockedProviders.add( provider );
			dpStatistics.put( provider, new DPStatistics() );
		}
	}

	private void cleanUp(SearchException originalException) {
		//release all readers and writers, then release locks
		SearchException raisedException = originalException;
		for (IndexReader reader : readers.values()) {
			try {
				reader.close();
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
		readers.clear();
		//TODO release lock of all indexes that do not need optimization early
		//don't optimze if there is a failure
		if ( raisedException == null ) {
			for (DirectoryProvider provider : lockedProviders) {
				Workspace.DPStatistics stats = dpStatistics.get( provider );
				if ( !stats.optimizationForced ) {
					OptimizerStrategy optimizerStrategy = searchFactoryImplementor.getOptimizerStrategy( provider );
					optimizerStrategy.addTransaction( stats.operations );
					try {
						optimizerStrategy.optimize( this );
					}
					catch (SearchException e) {
						raisedException = new SearchException( "Exception while optimizing directoryProvider: "
								+ provider.getDirectory().toString(), e );
						break; //no point in continuing
					}
				}
			}
		}
		for (IndexWriter writer : writers.values()) {
			try {
				writer.close();
			}
			catch (IOException e) {
				if ( raisedException != null ) {
					log.error( "Subsequent Exception while closing IndexWriter", e );
				}
				else {
					raisedException = new SearchException( "Exception while closing IndexWriter", e );
				}
			}
		}
		for (DirectoryProvider provider : lockedProviders) {
			searchFactoryImplementor.getLockableDirectoryProviders().get( provider ).unlock();
		}
		writers.clear();
		lockedProviders.clear();
		dpStatistics.clear();
		if ( raisedException != null ) throw raisedException;
	}

	/**
	 * release resources consumed in the workspace if any
	 */
	public void clean() {
		cleanUp( null );
	}

	public void optimize(DirectoryProvider provider) {
		OptimizerStrategy optimizerStrategy = searchFactoryImplementor.getOptimizerStrategy( provider );
		dpStatistics.get( provider ).optimizationForced = true;
		optimizerStrategy.optimizationForced();
	}

	private class DPStatistics {
		boolean optimizationForced = false;
		public long operations;
	}

	public boolean isBatch() {
		return isBatch;
	}

	public void setBatch(boolean isBatch) {
		this.isBatch = isBatch;
	}
}
