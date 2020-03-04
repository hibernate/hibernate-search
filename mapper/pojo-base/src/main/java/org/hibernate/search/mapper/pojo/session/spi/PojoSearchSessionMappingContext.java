/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.session.spi;

import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexer;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexingPlan;
import org.hibernate.search.mapper.pojo.work.spi.PojoWorkMappingContext;
import org.hibernate.search.mapper.pojo.work.spi.PojoWorkSessionContext;

/**
 * Mapping-scoped information and operations for use in POJO search sessions.
 */
public interface PojoSearchSessionMappingContext extends PojoWorkMappingContext {

	<R> PojoIndexingPlan<R> createIndexingPlan(PojoWorkSessionContext<R> context,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy);

	PojoIndexer createIndexer(PojoWorkSessionContext<?> context, DocumentCommitStrategy commitStrategy);

}
