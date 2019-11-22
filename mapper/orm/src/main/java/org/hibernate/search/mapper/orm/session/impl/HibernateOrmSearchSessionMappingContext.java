/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.session.impl;

import java.util.Collection;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.mapper.orm.mapping.context.impl.HibernateOrmMappingContextImpl;
import org.hibernate.search.mapper.orm.scope.impl.SearchScopeImpl;

public interface HibernateOrmSearchSessionMappingContext {

	FailureHandler getFailureHandler();

	HibernateOrmMappingContextImpl getBackendMappingContext();

	<T> SearchScopeImpl<T> createScope(Collection<? extends Class<? extends T>> types);

	<T> SearchScopeImpl<T> createScope(Class<T> expectedSuperType, Collection<String> hibernateOrmEntityNames);

	HibernateOrmSearchSession.HibernateOrmSearchSessionBuilder createSessionBuilder(
			SessionImplementor sessionImplementor);
}
