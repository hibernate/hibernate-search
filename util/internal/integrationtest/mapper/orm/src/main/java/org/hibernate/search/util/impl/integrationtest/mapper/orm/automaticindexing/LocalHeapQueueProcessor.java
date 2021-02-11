/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.mapper.orm.automaticindexing;

import java.util.concurrent.CompletableFuture;

import org.hibernate.Session;
import org.hibernate.search.engine.backend.common.spi.MultiEntityOperationExecutionReport;
import org.hibernate.search.engine.backend.orchestration.spi.BatchedWorkProcessor;
import org.hibernate.search.mapper.orm.automaticindexing.spi.AutomaticIndexingMappingContext;
import org.hibernate.search.mapper.orm.automaticindexing.spi.AutomaticIndexingQueueEventProcessingPlan;
import org.hibernate.search.mapper.orm.common.EntityReference;
import org.hibernate.search.mapper.pojo.route.DocumentRoutesDescriptor;
import org.hibernate.search.util.impl.test.SerializationUtils;

public class LocalHeapQueueProcessor implements BatchedWorkProcessor {

	private final AutomaticIndexingMappingContext mapping;

	private Session session;
	private AutomaticIndexingQueueEventProcessingPlan plan;

	public LocalHeapQueueProcessor(AutomaticIndexingMappingContext mapping) {
		this.mapping = mapping;
	}

	@Override
	public void beginBatch() {
		if ( session == null ) {
			session = mapping.sessionFactory().openSession();
			plan = mapping.createIndexingQueueEventProcessingPlan( session );
		}
	}

	public void process(LocalHeapQueueIndexingEvent event) {
		DocumentRoutesDescriptor routes = SerializationUtils.deserialize( DocumentRoutesDescriptor.class, event.routes );
		switch ( event.eventType ) {
			case ADD:
				plan.add( event.entityName, event.serializedId, routes );
				break;
			case ADD_OR_UPDATE:
				plan.addOrUpdate( event.entityName, event.serializedId, routes );
				break;
			case DELETE:
				plan.delete( event.entityName, event.serializedId, routes );
				break;
		}
	}

	@Override
	public CompletableFuture<?> endBatch() {
		return plan.executeAndReport().thenAccept( this::processReport );
	}

	private void processReport(MultiEntityOperationExecutionReport<EntityReference> report) {
		if ( report.throwable().isPresent() ) {
			Throwable cause = report.throwable().get();
			try {
				releaseSession();
			}
			catch (RuntimeException e) {
				cause.addSuppressed( e );
			}
			throw new IllegalStateException( "Reindexing failed for entities " + report.failingEntityReferences(),
					cause );
		}
	}

	@Override
	public void complete() {
		// Parking: release the session/connection
		releaseSession();
	}

	private void releaseSession() {
		try {
			if ( session != null ) {
				session.close();
			}
		}
		finally {
			session = null;
		}
	}
}
