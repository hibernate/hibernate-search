/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.session.impl;

import java.util.Collection;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.mapper.orm.loading.spi.HibernateOrmLoadingMappingContext;
import org.hibernate.search.mapper.orm.scope.impl.SearchScopeImpl;
import org.hibernate.search.mapper.pojo.session.spi.PojoSearchSessionMappingContext;
import org.hibernate.search.mapper.pojo.work.SearchIndexingPlanFilter;
import org.hibernate.search.mapper.pojo.work.spi.ConfiguredSearchIndexingPlanFilter;

public interface HibernateOrmSearchSessionMappingContext
		extends PojoSearchSessionMappingContext, HibernateOrmLoadingMappingContext {

	@Override
	FailureHandler failureHandler();

	<SR, T> SearchScopeImpl<SR, T> createScope(Collection<? extends Class<? extends T>> types);

	<SR, T> SearchScopeImpl<SR, T> createScope(Class<T> expectedSuperType, Collection<String> entityNames);

	HibernateOrmSearchSession.Builder createSessionBuilder(
			SessionImplementor sessionImplementor);

	ConfiguredSearchIndexingPlanFilter applicationIndexingPlanFilter();

	ConfiguredSearchIndexingPlanFilter configuredSearchIndexingPlanFilter(SearchIndexingPlanFilter filter);
}
