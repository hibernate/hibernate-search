/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.work.execution.impl;

import org.hibernate.search.backend.elasticsearch.orchestration.impl.ElasticsearchSerialWorkOrchestrator;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexer;
import org.hibernate.search.engine.backend.work.execution.spi.IndexWorkspace;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlan;
import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;

/**
 * An interface with knowledge of the backend internals,
 * able to create components related to work execution.
 * <p>
 * Note this interface exists mainly to more cleanly pass information
 * from the backend to the various work execution components.
 * If we just passed the backend to the various work execution components,
 * we would have a cyclic dependency.
 * If we passed all the components held by the backend to the various work execution components,
 * we would end up with methods with many parameters.
 */
public interface WorkExecutionBackendContext {

	IndexIndexingPlan createIndexingPlan(
			ElasticsearchSerialWorkOrchestrator orchestrator,
			WorkExecutionIndexManagerContext indexManagerContext,
			BackendSessionContext sessionContext,
			DocumentRefreshStrategy refreshStrategy);

	IndexIndexer createIndexer(
			ElasticsearchSerialWorkOrchestrator orchestrator,
			WorkExecutionIndexManagerContext indexManagerContext,
			BackendSessionContext sessionContext);

	IndexWorkspace createWorkspace(WorkExecutionIndexManagerContext indexManagerContext, String tenantId);

}
