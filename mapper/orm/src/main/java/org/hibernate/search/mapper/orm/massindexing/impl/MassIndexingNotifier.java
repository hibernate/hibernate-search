/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.massindexing.impl;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.atomic.LongAdder;

import org.hibernate.Session;
import org.hibernate.search.engine.reporting.EntityIndexingFailureContext;
import org.hibernate.search.engine.reporting.FailureContext;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.mapper.orm.common.EntityReference;
import org.hibernate.search.mapper.orm.common.impl.EntityReferenceImpl;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.mapper.orm.massindexing.monitor.MassIndexingMonitor;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

class MassIndexingNotifier {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final FailureHandler failureHandler;
	private final MassIndexingMonitor monitor;

	private final LongAdder failureCount = new LongAdder();

	MassIndexingNotifier(FailureHandler failureHandler,
			MassIndexingMonitor monitor) {
		this.failureHandler = failureHandler;
		this.monitor = monitor;
	}

	void notifyAddedTotalCount(long totalCount) {
		monitor.addToTotalCount( totalCount );
	}

	void notifyRunnableFailure(Exception exception, String operation) {
		FailureContext.Builder contextBuilder = FailureContext.builder();
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

	<T> void notifyEntityIndexingFailure(Class<T> entityType, String entityName,
			Session session, T entity, Throwable throwable) {
		failureCount.increment();
		EntityIndexingFailureContext.Builder contextBuilder = EntityIndexingFailureContext.builder();
		contextBuilder.throwable( throwable );
		// Add minimal information here, but information we're sure we can get
		contextBuilder.failingOperation( log.massIndexerIndexingInstance( entityName ) );
		// Add more information here, but information that may not be available if the session completely broke down
		// (we're being extra careful here because we don't want to throw an exception while handling and exception)
		EntityReference entityReference = extractReferenceOrSuppress( entityType, entityName, session, entity, throwable );
		if ( entityReference != null ) {
			contextBuilder.entityReference( entityReference );
		}
		failureHandler.handle( contextBuilder.build() );
	}

	void notifyIndexingComplete() {
		monitor.indexingCompleted();
	}

	private <T> EntityReference extractReferenceOrSuppress(Class<T> entityType, String entityName,
			Session session, Object entity, Throwable throwable) {
		try {
			return new EntityReferenceImpl( entityType, entityName, session.getIdentifier( entity ) );
		}
		catch (RuntimeException e) {
			// We failed to extract a reference.
			// Let's just give up and suppress the exception.
			throwable.addSuppressed( e );
			return null;
		}
	}
}
