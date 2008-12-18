package org.hibernate.search.backend.impl.lucene;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import org.apache.lucene.index.IndexWriter;
import org.slf4j.Logger;

import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.Workspace;
import org.hibernate.search.backend.impl.lucene.works.LuceneWorkVisitor;
import org.hibernate.search.util.LoggerFactory;

/**
 * A Runnable containing a unit of changes to be applied to a specific index.
 * After creation, use addWork(LuceneWork) to fill the changes queue and then
 * run it to apply all changes. After run() this object should be discarded.
 * @see Runnable
 * @see #addWork(LuceneWork)
 * @author Sanne Grinovero
 */
class PerDPQueueProcessor implements Runnable {
	
	private static final Logger log = LoggerFactory.make();
	private final Workspace workspace;
	private final LuceneWorkVisitor worker;
	private final ExecutorService executor;
	private final List<LuceneWork> workOnWriter = new ArrayList<LuceneWork>();
	
	// if any work needs batchmode, set corresponding flag to true:
	private boolean batchmode = false;
	
	/**
	 * @param resources All resources for the given DirectoryProvider are collected
	 *  from this wrapping object.
	 */
	public PerDPQueueProcessor(PerDPResources resources) {
		this.worker = resources.getVisitor();
		this.workspace = resources.getWorkspace();
		this.executor = resources.getExecutor();
	}

	/**
	 * adds a LuceneWork to the internal queue. Can't remove them.
	 * @param work
	 */
	public void addWork(LuceneWork work) {
		if ( work.isBatch() ) {
			batchmode = true;
			log.debug( "Batch mode enabled" );
		}
		workOnWriter.add( work );
	}

	/**
	 * Do all workOnWriter on an IndexWriter.
	 */
	public void run() {
		if ( workOnWriter.isEmpty() ) {
			return;
		}
		log.debug( "Opening an IndexWriter for update" );
		IndexWriter indexWriter = workspace.getIndexWriter( batchmode );
		try {
			for (LuceneWork lw : workOnWriter) {
				lw.getWorkDelegate( worker ).performWork( lw, indexWriter );
			}
			workspace.commitIndexWriter();
			//TODO skip this when indexing in batches:
			performOptimizations();
		}
		finally {
			workspace.closeIndexWriter();
		}
	}
	
	private void performOptimizations() {
		log.trace( "Locking Workspace (or waiting to...)" );
		workspace.lock();
		try {
			log.trace( "Workspace lock aquired." );
			//TODO next line is assuming the OptimizerStrategy will need an IndexWriter;
			// would be nicer to have the strategy put an OptimizeWork on the queue,
			// or just return "yes please" (true) to some method?
			//FIXME will not have a chance to trigger when no "add" activity is done.
			// this is correct until we enable modification counts for deletions too.
			workspace.optimizerPhase();
		}
		finally {
			workspace.unlock();
			log.trace( "Unlocked Workspace" );
		}
	}

	/**
	 * Each PerDPQueueProcessor is owned by an Executor,
	 * which contains the threads allowed to execute this.
	 * @return the Executor which should run this Runnable.
	 */
	public ExecutorService getOwningExecutor() {
		return executor;
	}

}
