/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.spi;

import java.util.Collection;

import org.hibernate.search.mapper.pojo.mapping.context.spi.AbstractPojoBackendMappingContext;
import org.hibernate.search.mapper.pojo.scope.spi.PojoScopeDelegate;
import org.hibernate.search.mapper.pojo.scope.spi.PojoScopeTypeExtendedContextProvider;
import org.hibernate.search.mapper.pojo.session.context.spi.AbstractPojoBackendSessionContext;
import org.hibernate.search.mapper.pojo.session.spi.PojoSearchSessionDelegate;

public interface PojoMappingDelegate extends AutoCloseable {

	@Override
	void close();

	<R, E, E2, C> PojoScopeDelegate<R, E2, C> createPojoScope(
			AbstractPojoBackendMappingContext mappingContext,
			Collection<? extends Class<? extends E>> targetedTypes,
			PojoScopeTypeExtendedContextProvider<E, C> indexedTypeExtendedContextProvider);

	PojoSearchSessionDelegate createSearchSessionDelegate(AbstractPojoBackendSessionContext backendSessionContext);

}
