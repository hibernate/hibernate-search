/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.query.impl;

import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;
import org.hibernate.search.engine.search.loading.spi.SearchLoadingContext;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;

/**
 * The context holding all the useful information pertaining to the Lucene search query,
 * to be used when extracting data from the response.
 */
class LuceneSearchQueryRequestContext {

	private final BackendSessionContext sessionContext;
	private final SearchLoadingContext<?> loadingContext;
	private final Query luceneQuery;
	private final Sort luceneSort;

	LuceneSearchQueryRequestContext(
			BackendSessionContext sessionContext,
			SearchLoadingContext<?> loadingContext,
			Query luceneQuery,
			Sort luceneSort) {
		this.sessionContext = sessionContext;
		this.loadingContext = loadingContext;
		this.luceneQuery = luceneQuery;
		this.luceneSort = luceneSort;
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

}
