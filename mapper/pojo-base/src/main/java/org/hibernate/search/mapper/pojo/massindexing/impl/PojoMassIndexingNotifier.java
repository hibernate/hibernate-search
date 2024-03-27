/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.massindexing.impl;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;

import org.hibernate.search.engine.common.EntityReference;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.massindexing.MassIndexingEntityFailureContext;
import org.hibernate.search.mapper.pojo.massindexing.MassIndexingFailureContext;
import org.hibernate.search.mapper.pojo.massindexing.MassIndexingFailureHandler;
import org.hibernate.search.mapper.pojo.massindexing.MassIndexingMonitor;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexingSessionContext;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * A central object to which various are reported,
 * responsible for notifying the user about these events.
 */
public class PojoMassIndexingNotifier {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final MassIndexingFailureHandler failureHandler;
	private final MassIndexingMonitor monitor;

	private final AtomicReference<RecordedFailure> firstFailure =
			new AtomicReference<>( null );
	private final LongAdder failureCount = new LongAdder();
	private final Map<String, AtomicLong> failureCounts = Collections.synchronizedMap( new HashMap<>() );
	private final long failureFloodingThreshold;

	public PojoMassIndexingNotifier(
			MassIndexingFailureHandler failureHandler, MassIndexingMonitor monitor, Long failureFloodingThreshold) {
		this.failureHandler = failureHandler;
		this.monitor = monitor;
		this.failureFloodingThreshold = Optional.ofNullable( failureFloodingThreshold )
				.orElseGet( failureHandler::failureFloodingThreshold );
	}

	void reportAddedTotalCount(long totalCount) {
		monitor.addToTotalCount( totalCount );
	}

	void reportError(Error error) {
		// Don't report the error anywhere: an Error is serious enough that we want to report it directly by bubbling up.
		// We're just recording that the first failure was an error so that later interruptions
		// don't trigger logging.
		RecordedFailure recordedFailure = new RecordedFailure( error );
		firstFailure.compareAndSet( null, recordedFailure );
	}

	void reportInterrupted(InterruptedException exception) {
		RecordedFailure recordedFailure = new RecordedFailure( exception );
		boolean isFirst = firstFailure.compareAndSet( null, recordedFailure );
		if ( isFirst ) {
			// Only log this once, and only if the interruption was the original failure.
			log.interruptedBatchIndexing();
		}
		// else: don't report the interruption, as it was most likely caused by a previous failure
	}

	void reportRunnableFailure(Exception exception, String operation) {
		recordFailure( exception, true );

		MassIndexingFailureContext.Builder contextBuilder = MassIndexingFailureContext.builder();
		contextBuilder.throwable( exception );
		contextBuilder.failingOperation( operation );
		failureHandler.handle( contextBuilder.build() );
	}

	void reportEntitiesLoaded(int size) {
		monitor.entitiesLoaded( size );
	}

	void reportDocumentBuilt() {
		monitor.documentsBuilt( 1 );
	}

	void reportDocumentsAdded(int size) {
		monitor.documentsAdded( size );
	}

	void reportEntityIndexingFailure(PojoMassIndexingIndexedTypeGroup<?> typeGroup,
			PojoMassIndexingSessionContext sessionContext, Object entity, Exception exception) {
		String failingOperation = log.massIndexerIndexingInstance( typeGroup.notifiedGroupName() );

		// Don't record these failures as suppressed beyond the first one, because there may be hundreds of them.
		RecordedFailure recordedFailure = recordFailure( exception, false );
		// We want to check this after recording failure above to make sure that total failure count is increased.
		if ( shouldNotBeReported( failingOperation ) ) {
			return;
		}

		MassIndexingEntityFailureContext.Builder contextBuilder = MassIndexingEntityFailureContext.builder();
		contextBuilder.throwable( exception );
		// Add minimal information here, but information we're sure we can get
		contextBuilder.failingOperation( failingOperation );
		// Add more information here, but information that may not be available if the session completely broke down
		// (we're being extra careful here because we don't want to throw an exception while handling and exception)
		EntityReference entityReference = extractReferenceOrSuppress( typeGroup, sessionContext, entity, exception );
		if ( entityReference != null ) {
			contextBuilder.failingEntityReference( entityReference );
			recordedFailure.entityReference = entityReference;
		}
		failureHandler.handle( contextBuilder.build() );
	}

	void reportEntitiesLoadingFailure(PojoMassIndexingIndexedTypeGroup<?> typeGroup, List<?> idList, Exception exception) {
		String failingOperation = log.massIndexingLoadingAndExtractingEntityData( typeGroup.notifiedGroupName() );

		// Don't record these failures as suppressed beyond the first one, because there may be hundreds of them.
		recordFailure( exception, false );

		// We want to check this after recording failure above to make sure that total failure count is increased.
		if ( shouldNotBeReported( failingOperation ) ) {
			return;
		}

		MassIndexingEntityFailureContext.Builder contextBuilder = MassIndexingEntityFailureContext.builder();
		contextBuilder.throwable( exception );
		// Add minimal information here, but information we're sure we can get
		contextBuilder.failingOperation( failingOperation );
		// Add more information here:
		for ( Object id : idList ) {
			try {
				contextBuilder.failingEntityReference( typeGroup.makeSuperTypeReference( id ) );
			}
			catch (Exception e) {
				exception.addSuppressed( e );
			}
		}
		failureHandler.handle( contextBuilder.build() );
	}

	private boolean shouldNotBeReported(String operation) {
		long failuresSoFar = failureCounts.computeIfAbsent(
				operation,
				s -> new AtomicLong( 0 )
		)
				.incrementAndGet();

		return failureFloodingThreshold < failuresSoFar;
	}

	void reportIndexingCompleted() {
		monitor.indexingCompleted();

		RecordedFailure firstFailure = this.firstFailure.get();
		if ( firstFailure == null ) {
			return;
		}

		// report that some failures went unreported:
		for ( Map.Entry<String, AtomicLong> entry : failureCounts.entrySet() ) {
			long unreported = entry.getValue().get() - failureFloodingThreshold;
			if ( unreported > 0 ) {
				MassIndexingFailureContext.Builder builder = MassIndexingFailureContext.builder();
				builder.throwable( log.notReportedFailures( unreported ) );
				builder.failingOperation( entry.getKey() );
				failureHandler.handle( builder.build() );
			}
		}

		if ( firstFailure.throwable instanceof InterruptedException ) {
			throw log.massIndexingThreadInterrupted(
					(InterruptedException) firstFailure.throwable
			);
		}
		else if ( firstFailure.entityReference != null ) {
			throw log.massIndexingFirstFailureOnEntity(
					failureCount.longValue(),
					firstFailure.entityReference,
					firstFailure.throwable.getMessage(), firstFailure.throwable
			);
		}
		else {
			throw log.massIndexingFirstFailure(
					failureCount.longValue(),
					firstFailure.throwable.getMessage(), firstFailure.throwable
			);
		}
	}

	private RecordedFailure recordFailure(Exception exception, boolean recordSuppressed) {
		RecordedFailure recordedFailure = new RecordedFailure( exception );
		boolean isFirst = firstFailure.compareAndSet( null, recordedFailure );
		failureCount.increment();
		if ( !isFirst && recordSuppressed ) {
			firstFailure.get().throwable.addSuppressed( exception );
		}
		return recordedFailure;
	}

	private EntityReference extractReferenceOrSuppress(PojoMassIndexingIndexedTypeGroup<?> typeGroup,
			PojoMassIndexingSessionContext sessionContext, Object entity, Throwable throwable) {
		try {
			return typeGroup.extractReference( sessionContext, entity );
		}
		catch (RuntimeException e) {
			// We failed to extract a reference.
			// Let's just give up and suppress the exception.
			throwable.addSuppressed( e );
			return null;
		}
	}

	private static class RecordedFailure {
		private final Throwable throwable;
		private volatile Object entityReference;

		RecordedFailure(Throwable throwable) {
			this.throwable = throwable;
		}
	}

}
