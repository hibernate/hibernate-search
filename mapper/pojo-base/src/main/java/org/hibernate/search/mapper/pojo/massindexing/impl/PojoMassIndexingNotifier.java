/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.massindexing.impl;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;

import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.massindexing.MassIndexingEntityFailureContext;
import org.hibernate.search.mapper.pojo.massindexing.MassIndexingFailureContext;
import org.hibernate.search.mapper.pojo.massindexing.MassIndexingFailureHandler;
import org.hibernate.search.mapper.pojo.massindexing.MassIndexingMonitor;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexingSessionContext;

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

	public PojoMassIndexingNotifier(
			MassIndexingFailureHandler failureHandler, MassIndexingMonitor monitor) {
		this.failureHandler = failureHandler;
		this.monitor = monitor;
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
		// Don't record these failures as suppressed beyond the first one, because there may be hundreds of them.
		RecordedFailure recordedFailure = recordFailure( exception, false );

		MassIndexingEntityFailureContext.Builder contextBuilder = MassIndexingEntityFailureContext.builder();
		contextBuilder.throwable( exception );
		// Add minimal information here, but information we're sure we can get
		contextBuilder.failingOperation( log.massIndexerIndexingInstance( typeGroup.notifiedGroupName() ) );
		// Add more information here, but information that may not be available if the session completely broke down
		// (we're being extra careful here because we don't want to throw an exception while handling and exception)
		Object entityReference = extractReferenceOrSuppress( typeGroup, sessionContext, entity, exception );
		if ( entityReference != null ) {
			contextBuilder.entityReference( entityReference );
			recordedFailure.entityReference = entityReference;
		}
		failureHandler.handle( contextBuilder.build() );
	}

	void reportEntitiesLoadingFailure(PojoMassIndexingIndexedTypeGroup<?> typeGroup, List<?> idList, Exception exception) {
		// Don't record these failures as suppressed beyond the first one, because there may be hundreds of them.
		recordFailure( exception, false );

		MassIndexingEntityFailureContext.Builder contextBuilder = MassIndexingEntityFailureContext.builder();
		contextBuilder.throwable( exception );
		// Add minimal information here, but information we're sure we can get
		contextBuilder.failingOperation( log.massIndexingLoadingAndExtractingEntityData( typeGroup.notifiedGroupName() ) );
		// Add more information here:
		for ( Object id : idList ) {
			try {
				contextBuilder.entityReference( typeGroup.makeSuperTypeReference( id ) );
			}
			catch (Exception e) {
				exception.addSuppressed( e );
			}
		}
		failureHandler.handle( contextBuilder.build() );
	}

	void reportIndexingCompleted() {
		monitor.indexingCompleted();

		RecordedFailure firstFailure = this.firstFailure.get();
		if ( firstFailure == null ) {
			return;
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

	private Object extractReferenceOrSuppress(PojoMassIndexingIndexedTypeGroup<?> typeGroup,
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
