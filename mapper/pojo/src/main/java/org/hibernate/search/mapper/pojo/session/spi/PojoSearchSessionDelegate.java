/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.session.spi;

import java.util.Collection;

import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.mapper.pojo.scope.spi.PojoScopeDelegate;
import org.hibernate.search.mapper.pojo.scope.spi.PojoScopeTypeExtendedContextProvider;
import org.hibernate.search.mapper.pojo.session.context.spi.AbstractPojoSessionContextImplementor;
import org.hibernate.search.mapper.pojo.work.spi.PojoSessionWorkExecutor;
import org.hibernate.search.mapper.pojo.work.spi.PojoWorkPlan;

public interface PojoSearchSessionDelegate {

	AbstractPojoSessionContextImplementor getSessionContext();

	<R, E, E2, C> PojoScopeDelegate<R, E2, C> createPojoScope(Collection<? extends Class<? extends E>> targetedTypes,
			PojoScopeTypeExtendedContextProvider<E, C> indexedTypeExtendedContextProvider);

	PojoWorkPlan createWorkPlan(DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy);

	PojoSessionWorkExecutor createSessionWorkExecutor(DocumentCommitStrategy commitStrategy);

}
