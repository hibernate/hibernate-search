/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.search.query.impl;

import java.util.Set;

import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;
import org.hibernate.search.engine.search.loading.spi.SearchLoadingContext;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;

/**
 * The context holding all the useful information pertaining to the Lucene search query,
 * to be used when extracting data from the response.
 */
class LuceneSearchQueryRequestContext {

	private final LuceneSearchQueryIndexScope<?> queryIndexScope;
	private final BackendSessionContext sessionContext;
	private final SearchLoadingContext<?> loadingContext;
	private final Query luceneQuery;
	private final Sort luceneSort;
	private final Set<String> routingKeys;

	LuceneSearchQueryRequestContext(
			LuceneSearchQueryIndexScope<?> queryIndexScope, BackendSessionContext sessionContext,
			SearchLoadingContext<?> loadingContext,
			Query luceneQuery,
			Sort luceneSort,
			Set<String> routingKeys) {
		this.queryIndexScope = queryIndexScope;
		this.sessionContext = sessionContext;
		this.loadingContext = loadingContext;
		this.luceneQuery = luceneQuery;
		this.luceneSort = luceneSort;
		this.routingKeys = routingKeys;
	}

	public LuceneSearchQueryIndexScope<?> getQueryIndexScope() {
		return queryIndexScope;
	}

	BackendSessionContext getSessionContext() {
		return sessionContext;
	}

	SearchLoadingContext<?> getLoadingContext() {
		return loadingContext;
	}

	Query getLuceneQuery() {
		return luceneQuery;
	}

	Sort getLuceneSort() {
		return luceneSort;
	}

	public Set<String> getRoutingKeys() {
		return routingKeys;
	}
}
