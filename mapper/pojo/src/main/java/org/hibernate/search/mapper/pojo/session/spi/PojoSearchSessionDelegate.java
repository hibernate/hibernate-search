/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.session.spi;

import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.mapper.pojo.session.context.spi.AbstractPojoBackendSessionContext;
import org.hibernate.search.mapper.pojo.work.spi.PojoSessionWorkExecutor;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexingPlan;

public interface PojoSearchSessionDelegate {

	AbstractPojoBackendSessionContext getBackendSessionContext();

	PojoIndexingPlan createIndexingPlan(DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy);

	PojoSessionWorkExecutor createSessionWorkExecutor(DocumentCommitStrategy commitStrategy);

}
