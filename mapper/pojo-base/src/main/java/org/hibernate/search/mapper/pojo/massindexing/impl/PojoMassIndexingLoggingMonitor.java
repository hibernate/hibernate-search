/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.massindexing.impl;

import java.lang.invoke.MethodHandles;
import java.time.Duration;
import java.util.OptionalLong;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.massindexing.MassIndexingMonitor;
import org.hibernate.search.mapper.pojo.massindexing.MassIndexingType;
import org.hibernate.search.mapper.pojo.massindexing.MassIndexingTypeGroupMonitor;
import org.hibernate.search.mapper.pojo.massindexing.MassIndexingTypeGroupMonitorContext;
import org.hibernate.search.mapper.pojo.massindexing.MassIndexingTypeGroupMonitorCreateContext;
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
	private boolean countOnStart;
	private boolean countOnBeforeType;

	private final AtomicLong typesToIndex = new AtomicLong();
	private final AtomicLong groupsWithUnknownTotal = new AtomicLong();

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
		this( logAfterNumberOfDocuments, false, true );
	}

	public PojoMassIndexingLoggingMonitor(boolean countOnStart, boolean countOnBeforeType) {
		this( 50, countOnStart, countOnBeforeType );
	}

	public PojoMassIndexingLoggingMonitor(int logAfterNumberOfDocuments, boolean countOnStart,
			boolean countOnBeforeType) {
		this.logAfterNumberOfDocuments = logAfterNumberOfDocuments;
		this.countOnStart = countOnStart;
		this.countOnBeforeType = countOnBeforeType;
	}

	@Override
	public MassIndexingTypeGroupMonitor typeGroupMonitor(MassIndexingTypeGroupMonitorCreateContext context) {
		typesToIndex.addAndGet( context.includedTypes().size() );
		return new MassIndexingTypeGroupMonitorImpl( context );
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
		if ( ( previous / period ) < ( current / period ) ) {
			long currentTime = System.nanoTime();
			printStatusMessage( startTime, currentTime, totalCounter.longValue(), current, typesToIndex.get(),
					groupsWithUnknownTotal.get() != 0 );
		}
	}

	@Override
	public void documentsBuilt(long number) {
		//not used
	}

	@Override
	public void entitiesLoaded(long size) {

	}

	@Override
	public void indexingCompleted() {
		log.indexingEntitiesCompleted( documentsDoneCounter.longValue(), totalCounter.longValue(),
				Duration.ofNanos( System.nanoTime() - startTime ) );
	}

	protected int getStatusMessagePeriod() {
		return logAfterNumberOfDocuments;
	}

	protected void printStatusMessage(long startTime, long currentTime, long totalTodoCount, long doneCount, long typesToIndex,
			boolean remainingUnknown) {
		StatusMessageInfo currentStatusMessageInfo = new StatusMessageInfo( currentTime, doneCount );
		StatusMessageInfo previousStatusMessageInfo = lastMessageInfo.getAndAccumulate(
				currentStatusMessageInfo,
				StatusMessageInfo.UPDATE_IF_MORE_UP_TO_DATE_FUNCTION
		);

		// Avoid logging outdated info if logging happened concurrently since we last called System.nanoTime()
		if ( !currentStatusMessageInfo.isMoreUpToDateThan( previousStatusMessageInfo ) ) {
			return;
		}

		long elapsedNano = currentTime - startTime;
		// period between two log events might be too short to use millis as a result infinity speed will be displayed.
		long intervalBetweenLogsNano = currentStatusMessageInfo.currentTime - previousStatusMessageInfo.currentTime;

		float estimateSpeed = doneCount * 1_000_000_000f / elapsedNano;
		float currentSpeed = ( currentStatusMessageInfo.documentsDone
				- previousStatusMessageInfo.documentsDone ) * 1_000_000_000f / intervalBetweenLogsNano;

		if ( remainingUnknown ) {
			if ( typesToIndex > 0 ) {
				log.indexingProgress( doneCount, typesToIndex, currentSpeed, estimateSpeed );
			}
			else {
				log.indexingProgress( doneCount, currentSpeed, estimateSpeed );
			}
		}
		else {
			float estimatePercentileComplete = doneCount * 100f / totalTodoCount;
			long remainingCount = totalTodoCount - doneCount;

			if ( typesToIndex > 0 ) {
				log.indexingProgress(
						estimatePercentileComplete, doneCount, totalTodoCount, currentSpeed, estimateSpeed,
						remainingCount, typesToIndex
				);
			}
			else {
				log.indexingProgressWithRemainingTime(
						estimatePercentileComplete, doneCount, totalTodoCount, currentSpeed, estimateSpeed,
						remainingCount, Duration.ofMillis( (long) ( ( remainingCount / currentSpeed ) * 1000 ) )
				);
			}
		}
	}

	private static class StatusMessageInfo {
		public static final BinaryOperator<StatusMessageInfo> UPDATE_IF_MORE_UP_TO_DATE_FUNCTION =
				(StatusMessageInfo storedVal,
						StatusMessageInfo newVal) -> newVal.isMoreUpToDateThan( storedVal ) ? newVal : storedVal;

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

	private class MassIndexingTypeGroupMonitorImpl implements MassIndexingTypeGroupMonitor {

		private final long numberOfTypes;
		private final OptionalLong totalBefore;
		private boolean totalUnknown = true;

		public MassIndexingTypeGroupMonitorImpl(MassIndexingTypeGroupMonitorCreateContext context) {
			this.numberOfTypes = context.includedTypes().size();
			if ( countOnStart ) {
				totalBefore = context.totalCount();
				if ( totalBefore.isPresent() ) {
					totalUnknown = false;
					long count = totalBefore.getAsLong();
					totalCounter.add( count );
					log.indexingEntitiesApprox( count, context.includedTypes().stream().map( MassIndexingType::entityName )
							.collect( Collectors.joining( ", ", "[ ", " ]" ) ) );
				}
			}
			else {
				totalBefore = OptionalLong.empty();
			}
		}

		@Override
		public void documentsIndexed(long increment) {
			if ( totalUnknown ) {
				totalCounter.add( increment );
			}
		}

		@Override
		public void indexingStarted(MassIndexingTypeGroupMonitorContext context) {
			typesToIndex.addAndGet( -numberOfTypes );

			if ( countOnBeforeType ) {
				OptionalLong totalCount = context.totalCount();
				if ( totalCount.isEmpty() ) {
					groupsWithUnknownTotal.incrementAndGet();
				}
				else {
					totalUnknown = false;
					long actual = totalCount.getAsLong();
					totalCounter.add( actual - totalBefore.orElse( 0 ) );
					log.indexingEntities( actual );
				}
			}
		}

		@Override
		public void indexingCompleted(MassIndexingTypeGroupMonitorContext context) {
			if ( totalUnknown ) {
				groupsWithUnknownTotal.decrementAndGet();
			}
		}
	}
}
