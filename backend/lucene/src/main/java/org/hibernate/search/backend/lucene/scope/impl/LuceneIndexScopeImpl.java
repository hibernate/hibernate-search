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
import org.hibernate.search.backend.lucene.search.aggregation.impl.LuceneSearchAggregationBuilderFactory;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.backend.lucene.search.predicate.impl.LuceneSearchPredicateBuilderFactory;
import org.hibernate.search.backend.lucene.search.projection.impl.LuceneSearchProjectionBuilderFactory;
import org.hibernate.search.backend.lucene.search.query.impl.LuceneSearchQueryBuilderFactory;
import org.hibernate.search.backend.lucene.search.query.impl.SearchBackendContext;
import org.hibernate.search.backend.lucene.search.sort.impl.LuceneSearchSortBuilderFactory;
import org.hibernate.search.engine.backend.mapping.spi.BackendMappingContext;
import org.hibernate.search.engine.backend.scope.spi.IndexScope;
import org.hibernate.search.engine.search.aggregation.spi.SearchAggregationBuilderFactory;

import org.apache.lucene.index.IndexReader;


public class LuceneIndexScopeImpl
		implements IndexScope, LuceneIndexScope {

	private final LuceneSearchIndexScope searchScope;
	private final LuceneSearchPredicateBuilderFactory searchPredicateFactory;
	private final LuceneSearchSortBuilderFactory searchSortFactory;
	private final LuceneSearchQueryBuilderFactory searchQueryFactory;
	private final LuceneSearchProjectionBuilderFactory searchProjectionFactory;
	private final LuceneSearchAggregationBuilderFactory searchAggregationFactory;

	public LuceneIndexScopeImpl(SearchBackendContext backendContext,
			BackendMappingContext mappingContext,
			Set<? extends LuceneScopeIndexManagerContext> indexManagerContexts) {
		this.searchScope = backendContext.createSearchContext( mappingContext, indexManagerContexts );
		this.searchPredicateFactory = new LuceneSearchPredicateBuilderFactory( searchScope );
		this.searchSortFactory = new LuceneSearchSortBuilderFactory( searchScope );
		this.searchProjectionFactory = new LuceneSearchProjectionBuilderFactory( searchScope );
		this.searchAggregationFactory = new LuceneSearchAggregationBuilderFactory( searchScope );
		this.searchQueryFactory = new LuceneSearchQueryBuilderFactory( backendContext, searchScope, this.searchProjectionFactory );
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[indexNames=" + searchScope.hibernateSearchIndexNames() + "]";
	}

	@Override
	public LuceneSearchIndexScope searchScope() {
		return searchScope;
	}

	@Override
	public LuceneSearchPredicateBuilderFactory searchPredicateBuilderFactory() {
		return searchPredicateFactory;
	}

	@Override
	public LuceneSearchSortBuilderFactory searchSortBuilderFactory() {
		return searchSortFactory;
	}

	@Override
	public LuceneSearchQueryBuilderFactory searchQueryBuilderFactory() {
		return searchQueryFactory;
	}

	@Override
	public LuceneSearchProjectionBuilderFactory searchProjectionFactory() {
		return searchProjectionFactory;
	}

	@Override
	public SearchAggregationBuilderFactory searchAggregationFactory() {
		return searchAggregationFactory;
	}

	@Override
	public IndexReader openIndexReader(Set<String> routingKeys) {
		Set<String> indexNames = searchScope.hibernateSearchIndexNames();
		Collection<? extends LuceneSearchIndexContext> indexManagerContexts = searchScope.indexes();
		return HibernateSearchMultiReader.open( indexNames, indexManagerContexts, routingKeys );
	}
}
