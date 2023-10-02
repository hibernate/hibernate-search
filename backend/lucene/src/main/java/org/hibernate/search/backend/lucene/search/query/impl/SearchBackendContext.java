/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.search.query.impl;

import java.util.Set;

import org.hibernate.search.backend.lucene.scope.model.impl.LuceneScopeIndexManagerContext;
import org.hibernate.search.backend.lucene.search.projection.impl.LuceneSearchProjection;
import org.hibernate.search.engine.backend.mapping.spi.BackendMappingContext;
import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;
import org.hibernate.search.engine.search.loading.spi.SearchLoadingContextBuilder;

/**
 * An interface with knowledge of the backend internals,
 * able to create components related to work execution.
 * <p>
 * Note this interface exists mainly to more cleanly pass information
 * from the backend to the various search-related components.
 * If we just passed the backend to the various search-related components,
 * we would have a cyclic dependency.
 * If we passed all the components held by the backend to the various search-related components,
 * we would end up with methods with many parameters.
 */
public interface SearchBackendContext {

	LuceneSearchQueryIndexScope<?> createSearchContext(BackendMappingContext mappingContext,
			Set<? extends LuceneScopeIndexManagerContext> indexManagerContexts);

	<H> LuceneSearchQueryBuilder<H> createSearchQueryBuilder(
			LuceneSearchQueryIndexScope<?> scope,
			BackendSessionContext sessionContext,
			SearchLoadingContextBuilder<?, ?> loadingContextBuilder,
			LuceneSearchProjection<H> rootProjection);

}
