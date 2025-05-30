/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.search.query.impl;

import java.util.Set;

import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexModel;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.ElasticsearchSearchProjection;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.SearchProjectionBackendContext;
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

	SearchProjectionBackendContext getSearchProjectionBackendContext();

	<SR> ElasticsearchSearchQueryIndexScope<SR, ?> createSearchContext(BackendMappingContext mappingContext,
			Class<SR> rootScopeType,
			Set<ElasticsearchIndexModel> indexModels);

	<H> ElasticsearchSearchQueryBuilder<H> createSearchQueryBuilder(
			ElasticsearchSearchIndexScope<?> scope,
			BackendSessionContext sessionContext,
			SearchLoadingContextBuilder<?, ?> loadingContextBuilder,
			ElasticsearchSearchProjection<H> rootProjection);

}
