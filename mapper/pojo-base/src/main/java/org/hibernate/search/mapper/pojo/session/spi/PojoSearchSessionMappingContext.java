/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.session.spi;

import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.mapper.pojo.scope.spi.PojoScopeMappingContext;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexingQueueEventProcessingPlan;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexingQueueEventSendingPlan;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexer;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexingPlan;
import org.hibernate.search.mapper.pojo.work.spi.PojoWorkMappingContext;
import org.hibernate.search.mapper.pojo.work.spi.PojoWorkSessionContext;

/**
 * Mapping-scoped information and operations for use in POJO search sessions.
 */
public interface PojoSearchSessionMappingContext extends PojoWorkMappingContext, PojoScopeMappingContext {

	PojoIndexingPlan createIndexingPlan(PojoWorkSessionContext context,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy);

	PojoIndexingPlan createIndexingPlan(PojoWorkSessionContext context, PojoIndexingQueueEventSendingPlan sink);

	PojoIndexer createIndexer(PojoWorkSessionContext context);

	PojoIndexingQueueEventProcessingPlan createIndexingQueueEventProcessingPlan(PojoWorkSessionContext context,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy,
			PojoIndexingQueueEventSendingPlan sendingPlan);

}
