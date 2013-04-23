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
package org.hibernate.search.impl;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.hibernate.search.batchindexing.MassIndexerProgressMonitor;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * A very simple implementation of {@code MassIndexerProgressMonitor} which
 * uses the logger at INFO level to output indexing speed statistics.
 *
 * @author Sanne Grinovero
 */
public class SimpleIndexingProgressMonitor implements MassIndexerProgressMonitor {

	private static final Log log = LoggerFactory.make();
	private final AtomicLong documentsDoneCounter = new AtomicLong();
	private final AtomicLong totalCounter = new AtomicLong();
	private volatile long startTime;
	private final int logAfterNumberOfDocuments;

	/**
	 * Logs progress of indexing job every 50 documents written.
	 */
	public SimpleIndexingProgressMonitor() {
		this( 50 );
	}

	/**
	 * Logs progress of indexing job every <code>logAfterNumberOfDocuments</code>
	 * documents written.
	 *
	 * @param logAfterNumberOfDocuments log each time the specified number of documents has been added
	 */
	public SimpleIndexingProgressMonitor(int logAfterNumberOfDocuments) {
		this.logAfterNumberOfDocuments = logAfterNumberOfDocuments;
	}

	public void entitiesLoaded(int size) {
		//not used
	}

	public void documentsAdded(long increment) {
		long current = documentsDoneCounter.addAndGet( increment );
		if ( current == increment ) {
			startTime = System.nanoTime();
		}
		if ( current % getStatusMessagePeriod() == 0 ) {
			printStatusMessage( startTime, totalCounter.get(), current );
		}
	}

	public void documentsBuilt(int number) {
		//not used
	}

	public void addToTotalCount(long count) {
		totalCounter.addAndGet( count );
		log.indexingEntities( count );
	}

	public void indexingCompleted() {
		log.indexingEntitiesCompleted( totalCounter.get() );
	}

	protected int getStatusMessagePeriod() {
		return logAfterNumberOfDocuments;
	}

	protected void printStatusMessage(long startTime, long totalTodoCount, long doneCount) {
		long elapsedMs = TimeUnit.NANOSECONDS.toMillis( System.nanoTime() - startTime );
		log.indexingDocumentsCompleted( doneCount, elapsedMs );
		float estimateSpeed = doneCount * 1000f / elapsedMs;
		float estimatePercentileComplete = doneCount * 100f / totalTodoCount;
		log.indexingSpeed( estimateSpeed, estimatePercentileComplete );
	}
}
