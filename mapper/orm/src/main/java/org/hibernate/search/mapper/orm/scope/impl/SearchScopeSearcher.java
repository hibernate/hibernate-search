/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.scope.impl;

import org.hibernate.search.engine.search.query.dsl.SearchQuerySelectStep;
import org.hibernate.search.mapper.orm.common.EntityReference;
import org.hibernate.search.mapper.orm.loading.impl.HibernateOrmSelectionLoadingContext;
import org.hibernate.search.mapper.orm.search.loading.dsl.SearchLoadingOptionsStep;

@SuppressWarnings("deprecation")
public interface SearchScopeSearcher<SR, E> {
	SearchQuerySelectStep<SR, ?, EntityReference, E, SearchLoadingOptionsStep, ?, ?> search(
			HibernateOrmScopeSessionContext sessionContext,
			HibernateOrmSelectionLoadingContext.Builder loadingContextBuilder);
}
