/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.session.impl;

import java.util.Collection;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.mapper.orm.loading.impl.LoadingMappingContext;
import org.hibernate.search.mapper.orm.scope.impl.SearchScopeImpl;
import org.hibernate.search.mapper.pojo.work.SearchIndexingPlanFilter;
import org.hibernate.search.mapper.pojo.work.spi.ConfiguredSearchIndexingPlanFilter;
import org.hibernate.search.mapper.pojo.session.spi.PojoSearchSessionMappingContext;

public interface HibernateOrmSearchSessionMappingContext
		extends PojoSearchSessionMappingContext, LoadingMappingContext {

	@Override
	FailureHandler failureHandler();

	SessionFactoryImplementor sessionFactory();

	<T> SearchScopeImpl<T> createScope(Collection<? extends Class<? extends T>> types);

	<T> SearchScopeImpl<T> createScope(Class<T> expectedSuperType, Collection<String> entityNames);

	HibernateOrmSearchSession.Builder createSessionBuilder(
			SessionImplementor sessionImplementor);

	ConfiguredSearchIndexingPlanFilter applicationIndexingPlanFilter();

	ConfiguredSearchIndexingPlanFilter configuredSearchIndexingPlanFilter(SearchIndexingPlanFilter filter);
}
