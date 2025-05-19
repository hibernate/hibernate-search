/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.scope.impl;

import java.util.Set;

import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexModel;
import org.hibernate.search.backend.elasticsearch.search.query.impl.ElasticsearchSearchQueryIndexScope;
import org.hibernate.search.backend.elasticsearch.search.query.impl.SearchBackendContext;
import org.hibernate.search.engine.backend.mapping.spi.BackendMappingContext;
import org.hibernate.search.engine.backend.scope.spi.IndexScope;

public class ElasticsearchIndexScope<SR>
		implements IndexScope<SR> {

	private final ElasticsearchSearchQueryIndexScope<SR, ?> searchScope;

	public ElasticsearchIndexScope(BackendMappingContext mappingContext, SearchBackendContext backendContext,
			Class<SR> rootScopeType,
			Set<ElasticsearchIndexModel> indexModels) {
		this.searchScope = backendContext.createSearchContext( mappingContext, rootScopeType, indexModels );
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[indexNames=" + searchScope.hibernateSearchIndexNames() + "]";
	}

	@Override
	public ElasticsearchSearchQueryIndexScope<SR, ?> searchScope() {
		return searchScope;
	}

}
