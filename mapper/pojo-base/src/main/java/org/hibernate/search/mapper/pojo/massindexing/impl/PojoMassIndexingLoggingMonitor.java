/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.massindexing.impl;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.BinaryOperator;

import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.massindexing.MassIndexingMonitor;

import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * A very simple implementation of {@code MassIndexerProgressMonitor} which
 * uses the logger at INFO level to output indexing speed statistics.
 *
 * @author Sanne Grinovero
 */
public class PojoMassIndexingLoggingMonitor implements MassIndexingMonitor {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );
	private final AtomicLong documentsDoneCounter = new AtomicLong();
	private final AtomicReference<StatusMessageInfo> lastMessageInfo = new AtomicReference<>();
	private final LongAdder totalCounter = new LongAdder();
	private volatile long startTime;
	private final int logAfterNumberOfDocuments;

	/**
	 * Logs progress of indexing job every 50 documents written.
	 */
	public PojoMassIndexingLoggingMonitor() {
		this( 50 );
	}

	/**
	 * Logs progress of indexing job every {@code logAfterNumberOfDocuments}
	 * documents written.
	 *
	 * @param logAfterNumberOfDocuments log each time the specified number of documents has been added
	 */
	public PojoMassIndexingLoggingMonitor(int logAfterNumberOfDocuments) {
		this.logAfterNumberOfDocuments = logAfterNumberOfDocuments;
	}

	@Override
	public void documentsAdded(long increment) {
		if ( startTime == 0 ) {
			// this sync block doesn't seem to be a problem for Loom:
			// - always executed in MassIndexer threads, which are not virtual threads
			// - no I/O and simple in-memory operations
			synchronized (this) {
				if ( startTime == 0 ) {
					long theStartTime = System.nanoTime();
					lastMessageInfo.set( new StatusMessageInfo( startTime, 0 ) );
					// Do this last, so other threads will block until we're done initializing lastMessageInfo.
					startTime = theStartTime;
				}
			}
		}

		long previous = documentsDoneCounter.getAndAdd( increment );
		/*
		 * Only log if the current increment was the one that made the counter
		 * go to a higher multiple of the period.
		 */
		long current = previous + increment;
		int period = getStatusMessagePeriod();
		if ( (previous / period) < (current / period) ) {
			long currentTime = System.nanoTime();
			printStatusMessage( startTime, currentTime, totalCounter.longValue(), current );
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

	protected void printStatusMessage(long startTime, long currentTime, long totalTodoCount, long doneCount) {
		StatusMessageInfo currentStatusMessageInfo = new StatusMessageInfo( currentTime, doneCount );
		StatusMessageInfo previousStatusMessageInfo = lastMessageInfo.getAndAccumulate( currentStatusMessageInfo,
				StatusMessageInfo.UPDATE_IF_MORE_UP_TO_DATE_FUNCTION );

		// Avoid logging outdated info if logging happened concurrently since we last called System.nanoTime()
		if ( !currentStatusMessageInfo.isMoreUpToDateThan( previousStatusMessageInfo ) ) {
			return;
		}

		long elapsedNano = currentTime - startTime;
		// period between two log events might be too short to use millis as a result infinity speed will be displayed.
		long intervalBetweenLogsNano = currentStatusMessageInfo.currentTime - previousStatusMessageInfo.currentTime;

		log.indexingProgressRaw( doneCount, TimeUnit.NANOSECONDS.toMillis( elapsedNano ) );
		float estimateSpeed = doneCount * 1_000_000_000f / elapsedNano;
		float currentSpeed = ( currentStatusMessageInfo.documentsDone - previousStatusMessageInfo.documentsDone ) * 1_000_000_000f / intervalBetweenLogsNano;
		float estimatePercentileComplete = doneCount * 100f / totalTodoCount;
		log.indexingProgressStats( currentSpeed, estimateSpeed, estimatePercentileComplete );
	}

	private static class StatusMessageInfo {
		public static final BinaryOperator<StatusMessageInfo> UPDATE_IF_MORE_UP_TO_DATE_FUNCTION =
				(StatusMessageInfo storedVal, StatusMessageInfo newVal) ->
						newVal.isMoreUpToDateThan( storedVal ) ? newVal : storedVal;

		public final long currentTime;
		public final long documentsDone;

		public StatusMessageInfo(long currentTime, long documentsDone) {
			this.currentTime = currentTime;
			this.documentsDone = documentsDone;
		}

		public boolean isMoreUpToDateThan(StatusMessageInfo other) {
			return documentsDone > other.documentsDone
					// Ensure we log status updates even if the mass indexer is stuck for a long time
					|| documentsDone == other.documentsDone && currentTime > other.currentTime;
		}
	}
}
