/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.index.impl;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexModel;
import org.hibernate.search.backend.elasticsearch.logging.impl.QueryLog;
import org.hibernate.search.backend.elasticsearch.scope.impl.ElasticsearchIndexScope;
import org.hibernate.search.engine.backend.mapping.spi.BackendMappingContext;
import org.hibernate.search.engine.backend.scope.spi.IndexScope;
import org.hibernate.search.engine.backend.scope.spi.IndexScopeBuilder;

class ElasticsearchIndexScopeBuilder<SR> implements IndexScopeBuilder<SR> {

	private final IndexManagerBackendContext backendContext;
	private final BackendMappingContext mappingContext;
	private final Class<SR> rootScopeType;

	// Use LinkedHashSet to ensure stable order when generating requests
	private final Set<ElasticsearchIndexManagerImpl> indexManagers = new LinkedHashSet<>();

	ElasticsearchIndexScopeBuilder(IndexManagerBackendContext backendContext,
			BackendMappingContext mappingContext, Class<SR> rootScopeType, ElasticsearchIndexManagerImpl indexManager) {
		this.backendContext = backendContext;
		this.mappingContext = mappingContext;
		this.rootScopeType = rootScopeType;
		this.indexManagers.add( indexManager );
	}

	void add(IndexManagerBackendContext backendContext, ElasticsearchIndexManagerImpl indexManager) {
		if ( !this.backendContext.equals( backendContext ) ) {
			throw QueryLog.INSTANCE.cannotMixElasticsearchScopeWithOtherBackend(
					this, indexManager, backendContext.getEventContext()
			);
		}
		indexManagers.add( indexManager );
	}

	@Override
	public IndexScope<SR> build() {
		// Use LinkedHashSet to ensure stable order when generating requests
		Set<ElasticsearchIndexModel> indexModels = indexManagers.stream().map( ElasticsearchIndexManagerImpl::model )
				.collect( Collectors.toCollection( LinkedHashSet::new ) );
		return new ElasticsearchIndexScope<>( mappingContext, backendContext, rootScopeType, indexModels );
	}

	@Override
	public String toString() {
		return new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "backendContext=" ).append( backendContext )
				.append( ", indexManagers=" ).append( indexManagers )
				.append( "]" )
				.toString();
	}

}
