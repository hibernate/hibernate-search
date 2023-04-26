/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.automaticindexing.spi;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.search.engine.backend.common.spi.EntityReferenceFactory;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.engine.reporting.FailureHandler;

public interface AutomaticIndexingMappingContext {

	/**
	 * @return The Hibernate ORM {@link SessionFactory}.
	 */
	SessionFactoryImplementor sessionFactory();

	/**
	 * @return A failure handler, to report indexing errors in background processes.
	 */
	FailureHandler failureHandler();

	/**
	 * @param session A Hibernate ORM {@link Session} created from the same {@link #sessionFactory()} as this mapping.
	 * @return An event processing plan for the given session.
	 * It will not get executed automatically: you need to call {@link AutomaticIndexingQueueEventProcessingPlan#executeAndReport(OperationSubmitter)},
	 * which is asynchronous and returns a future.
	 */
	AutomaticIndexingQueueEventProcessingPlan createIndexingQueueEventProcessingPlan(Session session);

	EntityReferenceFactory entityReferenceFactory();

}
