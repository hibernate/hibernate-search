/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
