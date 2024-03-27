/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.automaticindexing.spi;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.search.engine.backend.common.spi.EntityReferenceFactory;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.mapper.orm.tenancy.spi.TenancyConfiguration;

public interface AutomaticIndexingMappingContext {

	/**
	 * @return The Hibernate ORM {@link SessionFactory}.
	 */
	SessionFactoryImplementor sessionFactory();

	/**
	 * @return A failure handler, to report indexing errors in background processes.
	 */
	FailureHandler failureHandler();

	TenancyConfiguration tenancyConfiguration();

	/**
	 * @param session A Hibernate ORM {@link Session} created from the same {@link #sessionFactory()} as this mapping.
	 * @return An event processing plan for the given session.
	 * It will not get executed automatically: you need to call {@link AutomaticIndexingQueueEventProcessingPlan#executeAndReport(OperationSubmitter)},
	 * which is asynchronous and returns a future.
	 */
	AutomaticIndexingQueueEventProcessingPlan createIndexingQueueEventProcessingPlan(Session session);

	EntityReferenceFactory entityReferenceFactory();

}
