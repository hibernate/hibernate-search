/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.index.impl;

import java.util.LinkedHashSet;
import java.util.Set;

import org.hibernate.search.backend.lucene.logging.impl.QueryLog;
import org.hibernate.search.backend.lucene.scope.impl.LuceneIndexScopeImpl;
import org.hibernate.search.engine.backend.mapping.spi.BackendMappingContext;
import org.hibernate.search.engine.backend.scope.spi.IndexScope;
import org.hibernate.search.engine.backend.scope.spi.IndexScopeBuilder;

class LuceneIndexScopeBuilder<SR> implements IndexScopeBuilder<SR> {

	private final IndexManagerBackendContext backendContext;
	private final BackendMappingContext mappingContext;
	private final Class<SR> rootScopeType;

	// Use LinkedHashSet to ensure stable order when generating requests
	private final Set<LuceneIndexManagerImpl> indexManagers = new LinkedHashSet<>();

	LuceneIndexScopeBuilder(IndexManagerBackendContext backendContext, BackendMappingContext mappingContext,
			Class<SR> rootScopeType,
			LuceneIndexManagerImpl indexManager) {
		this.backendContext = backendContext;
		this.mappingContext = mappingContext;
		this.rootScopeType = rootScopeType;
		this.indexManagers.add( indexManager );
	}

	void add(IndexManagerBackendContext backendContext, LuceneIndexManagerImpl indexManager) {
		if ( !this.backendContext.equals( backendContext ) ) {
			throw QueryLog.INSTANCE.cannotMixLuceneScopeWithOtherBackend(
					this, indexManager, backendContext.getEventContext()
			);
		}
		indexManagers.add( indexManager );
	}

	@Override
	public IndexScope<SR> build() {
		return new LuceneIndexScopeImpl<>( backendContext, mappingContext, rootScopeType, indexManagers );
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
