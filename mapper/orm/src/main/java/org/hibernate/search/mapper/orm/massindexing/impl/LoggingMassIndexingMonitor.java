/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.massindexing.impl;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.mapper.orm.massindexing.MassIndexingMonitor;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * A very simple implementation of {@code MassIndexerProgressMonitor} which
 * uses the logger at INFO level to output indexing speed statistics.
 *
 * @author Sanne Grinovero
 */
public class LoggingMassIndexingMonitor implements MassIndexingMonitor {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );
	private final AtomicLong documentsDoneCounter = new AtomicLong();
	private final LongAdder totalCounter = new LongAdder();
	private volatile long startTime;
	private final int logAfterNumberOfDocuments;

	/**
	 * Logs progress of indexing job every 50 documents written.
	 */
	public LoggingMassIndexingMonitor() {
		this( 50 );
	}

	/**
	 * Logs progress of indexing job every {@code logAfterNumberOfDocuments}
	 * documents written.
	 *
	 * @param logAfterNumberOfDocuments log each time the specified number of documents has been added
	 */
	public LoggingMassIndexingMonitor(int logAfterNumberOfDocuments) {
		this.logAfterNumberOfDocuments = logAfterNumberOfDocuments;
	}

	@Override
	public void documentsAdded(long increment) {
		long previous = documentsDoneCounter.getAndAdd( increment );
		if ( startTime == 0 ) {
			synchronized ( this ) {
				if ( startTime == 0 ) {
					startTime = System.nanoTime();
				}
			}
		}
		/*
		 * Only log if the current increment was the one that made the counter
		 * go to a higher multiple of the period.
		 */
		long current = previous + increment;
		int period = getStatusMessagePeriod();
		if ( ( previous / period ) < ( current / period ) ) {
			printStatusMessage( startTime, totalCounter.longValue(), current );
		}
	}

	@Override
	public void documentsBuilt(long number) {
		//not used
	}

	@Override
	public void entitiesLoaded(long size) {
		//not used
	}

	@Override
	public void addToTotalCount(long count) {
		totalCounter.add( count );
		log.indexingEntities( count );
	}

	@Override
	public void indexingCompleted() {
		log.indexingEntitiesCompleted( totalCounter.longValue() );
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
