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
package org.hibernate.search.backend.impl.lucene;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.LockObtainFailedException;
import org.slf4j.Logger;

import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.Workspace;
import org.hibernate.search.backend.impl.lucene.works.LuceneWorkVisitor;
import org.hibernate.search.exception.ErrorHandler;
import org.hibernate.search.exception.impl.ErrorContextBuilder;
import org.hibernate.search.util.LoggerFactory;

/**
 * A Runnable containing a unit of changes to be applied to a specific index.
 * After creation, use addWork(LuceneWork) to fill the changes queue and then
 * run it to apply all changes. After run() this object should be discarded.
 * 
 * A new PerDPQueueProcessor is created for each unit of work, expensive
 * resources to be shared across multiple transactions should be created once
 * in PerDPResources.
 * 
 * @see Runnable
 * @see #addWork(LuceneWork)
 * @author Sanne Grinovero
 */
class PerDPQueueProcessor implements Runnable {
	
	private static final Logger log = LoggerFactory.make();
	
	private final Workspace workspace;
	private final LuceneWorkVisitor worker;
	private final ExecutorService executor;
	private final boolean exclusiveIndexUsage;
	private final List<LuceneWork> workOnWriter = new ArrayList<LuceneWork>();
	private final ErrorHandler handler;
	
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
		this.exclusiveIndexUsage = resources.isExclusiveIndexUsageEnabled();
		this.handler = resources.getErrorHandler();
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
		ErrorContextBuilder errorContextBuilder = new ErrorContextBuilder();
		errorContextBuilder.allWorkToBeDone( workOnWriter );
		try {
			IndexWriter indexWriter = workspace.getIndexWriter( batchmode, errorContextBuilder );
			try {
				for ( LuceneWork lw : workOnWriter ) {
					lw.getWorkDelegate( worker ).performWork( lw, indexWriter );
					errorContextBuilder.workCompleted( lw );
				}
				workspace.commitIndexWriter( errorContextBuilder );
				performOptimizations();
			}
			finally {
				if ( ! exclusiveIndexUsage ) workspace.closeIndexWriter();
			}
		}
		catch ( Throwable tw ) {
			//needs to be attempted even for out of memory errors, therefore we catch Throwable
			log.error( "Unexpected error in Lucene Backend: ", tw );
			try {
				handler.handle( errorContextBuilder.errorThatOccurred( tw ).createErrorContext() );
				workspace.closeIndexWriter();
			}
			finally {
				//LockObtainFailedException is wrapped in a SearchException by the workspace
				//LockObtainFailedException should never ever release the lock
				if ( ! (tw.getCause() instanceof LockObtainFailedException) ) {
					workspace.forceLockRelease();
				}
			}
		}
	}
	
	private void performOptimizations() {
		//TODO next line is assuming the OptimizerStrategy will need an IndexWriter;
		// would be nicer to have the strategy put an OptimizeWork on the queue,
		// or just return "yes please" (true) to some method?
		//FIXME will not have a chance to trigger when no "add" activity is done.
		// this is correct until we enable modification counts for deletions too.
		workspace.optimizerPhase();
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
