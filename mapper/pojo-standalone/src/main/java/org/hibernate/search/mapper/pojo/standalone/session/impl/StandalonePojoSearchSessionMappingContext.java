/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.standalone.session.impl;

import java.util.Collection;

import org.hibernate.search.engine.backend.common.spi.EntityReferenceFactory;
import org.hibernate.search.mapper.pojo.standalone.common.EntityReference;
import org.hibernate.search.mapper.pojo.standalone.massindexing.impl.StandalonePojoMassIndexingMappingContext;
import org.hibernate.search.mapper.pojo.standalone.scope.impl.StandalonePojoScopeMappingContext;
import org.hibernate.search.mapper.pojo.standalone.scope.impl.SearchScopeImpl;
import org.hibernate.search.mapper.pojo.session.spi.PojoSearchSessionMappingContext;

public interface StandalonePojoSearchSessionMappingContext
		extends PojoSearchSessionMappingContext, StandalonePojoScopeMappingContext,
		StandalonePojoMassIndexingMappingContext {

	@Override
	EntityReferenceFactory<EntityReference> entityReferenceFactory();

	<T> SearchScopeImpl<T> createScope(Collection<? extends Class<? extends T>> types);

}
