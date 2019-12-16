/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.spi;

import java.util.Collection;

import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.scope.spi.PojoScopeMappingContext;
import org.hibernate.search.mapper.pojo.scope.spi.PojoScopeDelegate;
import org.hibernate.search.mapper.pojo.scope.spi.PojoScopeTypeExtendedContextProvider;
import org.hibernate.search.mapper.pojo.session.context.spi.AbstractPojoBackendSessionContext;
import org.hibernate.search.mapper.pojo.session.spi.PojoSearchSessionDelegate;
import org.hibernate.search.engine.environment.thread.spi.ThreadPoolProvider;

public interface PojoMappingDelegate extends AutoCloseable {

	@Override
	void close();

	ThreadPoolProvider getThreadPoolProvider();

	FailureHandler getFailureHandler();

	<R, E, E2, C> PojoScopeDelegate<R, E2, C> createPojoScope(
			PojoScopeMappingContext mappingContext,
			Collection<? extends PojoRawTypeIdentifier<? extends E>> targetedTypes,
			PojoScopeTypeExtendedContextProvider<E, C> indexedTypeExtendedContextProvider);

	PojoSearchSessionDelegate createSearchSessionDelegate(AbstractPojoBackendSessionContext backendSessionContext);

}
