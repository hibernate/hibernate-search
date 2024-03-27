/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.scope.impl;

import java.util.Collection;
import java.util.Set;

import org.hibernate.search.backend.lucene.lowlevel.reader.impl.HibernateSearchMultiReader;
import org.hibernate.search.backend.lucene.scope.LuceneIndexScope;
import org.hibernate.search.backend.lucene.scope.model.impl.LuceneScopeIndexManagerContext;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexContext;
import org.hibernate.search.backend.lucene.search.query.impl.LuceneSearchQueryIndexScope;
import org.hibernate.search.backend.lucene.search.query.impl.SearchBackendContext;
import org.hibernate.search.engine.backend.mapping.spi.BackendMappingContext;
import org.hibernate.search.engine.backend.scope.spi.IndexScope;

import org.apache.lucene.index.IndexReader;

public class LuceneIndexScopeImpl
		implements IndexScope, LuceneIndexScope {

	private final LuceneSearchQueryIndexScope<?> searchScope;

	public LuceneIndexScopeImpl(SearchBackendContext backendContext,
			BackendMappingContext mappingContext,
			Set<? extends LuceneScopeIndexManagerContext> indexManagerContexts) {
		this.searchScope = backendContext.createSearchContext( mappingContext, indexManagerContexts );
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[indexNames=" + searchScope.hibernateSearchIndexNames() + "]";
	}

	@Override
	public LuceneSearchQueryIndexScope<?> searchScope() {
		return searchScope;
	}

	@Override
	public IndexReader openIndexReader(Set<String> routingKeys) {
		Set<String> indexNames = searchScope.hibernateSearchIndexNames();
		Collection<? extends LuceneSearchIndexContext> indexManagerContexts = searchScope.indexes();
		return HibernateSearchMultiReader.open( indexNames, indexManagerContexts, routingKeys );
	}
}
