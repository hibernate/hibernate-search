/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.massindexing.impl;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;

import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.massindexing.MassIndexingEntityFailureContext;
import org.hibernate.search.mapper.pojo.massindexing.MassIndexingFailureContext;
import org.hibernate.search.mapper.pojo.massindexing.MassIndexingFailureHandler;
import org.hibernate.search.mapper.pojo.massindexing.MassIndexingMonitor;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexingSessionContext;

public class PojoMassIndexingNotifier {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final MassIndexingFailureHandler failureHandler;
	private final MassIndexingMonitor monitor;

	private final AtomicReference<RecordedEntityIndexingFailure> entityIndexingFirstFailure
			= new AtomicReference<>( null );
	private final LongAdder entityIndexingFailureCount = new LongAdder();

	public PojoMassIndexingNotifier(
			MassIndexingFailureHandler failureHandler, MassIndexingMonitor monitor) {
		this.failureHandler = failureHandler;
		this.monitor = monitor;
	}

	void notifyAddedTotalCount(long totalCount) {
		monitor.addToTotalCount( totalCount );
	}

	void notifyRunnableFailure(Exception exception, String operation) {
		MassIndexingFailureContext.Builder contextBuilder = MassIndexingFailureContext.builder();
		contextBuilder.throwable( exception );
		contextBuilder.failingOperation( operation );
		failureHandler.handle( contextBuilder.build() );
	}

	void notifyEntitiesLoaded(int size) {
		monitor.entitiesLoaded( size );
	}

	void notifyDocumentBuilt() {
		monitor.documentsBuilt( 1 );
	}

	void notifyDocumentsAdded(int size) {
		monitor.documentsAdded( size );
	}

	void notifyEntityIndexingFailure(PojoMassIndexingIndexedTypeGroup<?, ?> typeGroup,
			PojoMassIndexingSessionContext sessionContext, Object entity, Exception exception) {
		RecordedEntityIndexingFailure recordedFailure = new RecordedEntityIndexingFailure( exception );
		entityIndexingFirstFailure.compareAndSet( null, recordedFailure );
		entityIndexingFailureCount.increment();

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

	void notifyIndexingCompletedSuccessfully() {
		monitor.indexingCompleted();

		SearchException entityIndexingException = createEntityIndexingExceptionOrNull();
		if ( entityIndexingException != null ) {
			throw entityIndexingException;
		}
	}

	void notifyIndexingCompletedWithInterruption() {
		log.interruptedBatchIndexing();
		notifyIndexingCompletedSuccessfully();
	}

	void notifyIndexingCompletedWithFailure(Throwable throwable) {
		// TODO HSEARCH-3729 Call a different method when indexing failed?
		monitor.indexingCompleted();

		SearchException entityIndexingException = createEntityIndexingExceptionOrNull();
		if ( entityIndexingException != null ) {
			throwable.addSuppressed( entityIndexingException );
		}

		MassIndexingFailureContext.Builder contextBuilder = MassIndexingFailureContext.builder();
		contextBuilder.throwable( throwable );
		contextBuilder.failingOperation( log.massIndexerOperation() );
		failureHandler.handle( contextBuilder.build() );
	}

	private SearchException createEntityIndexingExceptionOrNull() {
		RecordedEntityIndexingFailure firstFailure = entityIndexingFirstFailure.get();
		if ( firstFailure == null ) {
			return null;
		}
		return log.massIndexingEntityFailures(
				entityIndexingFailureCount.longValue(),
				firstFailure.entityReference,
				firstFailure.throwable.getMessage(), firstFailure.throwable
		);
	}

	private Object extractReferenceOrSuppress(PojoMassIndexingIndexedTypeGroup<?, ?> typeGroup,
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

	private static class RecordedEntityIndexingFailure {
		private Throwable throwable;
		private Object entityReference;

		RecordedEntityIndexingFailure(Throwable throwable) {
			this.throwable = throwable;
		}
	}
}
