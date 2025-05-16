/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.standalone.scope.impl;

import org.hibernate.search.engine.common.EntityReference;
import org.hibernate.search.engine.search.query.dsl.SearchQuerySelectStep;
import org.hibernate.search.mapper.pojo.loading.spi.PojoSelectionLoadingContextBuilder;
import org.hibernate.search.mapper.pojo.scope.spi.PojoScopeSessionContext;

public interface SearchScopeSearcher<SR, E> {
	SearchQuerySelectStep<SR, ?, EntityReference, E, ?, ?, ?> search(PojoScopeSessionContext sessionContext,
			PojoSelectionLoadingContextBuilder<?> loadingContextBuilder);
}
