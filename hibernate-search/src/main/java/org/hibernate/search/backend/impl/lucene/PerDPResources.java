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

import org.hibernate.search.batchindexing.impl.Executors;
import org.hibernate.search.spi.WorkerBuildContext;
import org.hibernate.search.backend.Workspace;
import org.hibernate.search.backend.impl.lucene.works.LuceneWorkVisitor;
import org.hibernate.search.exception.ErrorHandler;
import org.hibernate.search.indexes.impl.DirectoryBasedIndexManager;
import org.hibernate.search.util.logging.impl.LoggerFactory;
import org.hibernate.search.util.logging.impl.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Collects all resources needed to apply changes to one index,
 * and are reused across several WorkQueues.
 *
 * @author Sanne Grinovero
 */
class PerDPResources {
	
	private static final Log log = LoggerFactory.make();
	
	private final ExecutorService executor;
	private final LuceneWorkVisitor visitor;
	private final Workspace workspace;
	private final boolean exclusiveIndexUsage;
	private final ErrorHandler errorHandler;
	
	PerDPResources(WorkerBuildContext context, DirectoryBasedIndexManager indexManager) {
		errorHandler = context.getErrorHandler();
		workspace = new Workspace( indexManager, errorHandler );
		visitor = new LuceneWorkVisitor( workspace );
		int maxQueueLength = indexManager.getMaxQueueLength();
		executor = Executors.newFixedThreadPool( 1, "Directory writer", maxQueueLength );
		exclusiveIndexUsage = indexManager.isExclusiveIndexUsage();
	}

	public ExecutorService getExecutor() {
		return executor;
	}

	public LuceneWorkVisitor getVisitor() {
		return visitor;
	}

	public Workspace getWorkspace() {
		return workspace;
	}

	public boolean isExclusiveIndexUsageEnabled() {
		return exclusiveIndexUsage;
	}

	public void shutdown() {
		//sets the index to be closed after all current jobs are processed:
		if ( exclusiveIndexUsage ) {
			executor.execute( new CloseIndexRunnable( workspace ) );
		}
		executor.shutdown();
		try {
			executor.awaitTermination( Long.MAX_VALUE, TimeUnit.SECONDS );
		}
		catch (InterruptedException e) {
			log.interruptedWhileWaitingForIndexActivity();
		}
	}

	public ErrorHandler getErrorHandler() {
		return errorHandler;
	}
	
}
