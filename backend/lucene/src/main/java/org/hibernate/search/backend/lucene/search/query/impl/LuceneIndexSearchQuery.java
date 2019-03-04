/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.query.impl;

import java.util.Set;

import org.hibernate.search.backend.lucene.index.spi.ReaderProvider;
import org.hibernate.search.backend.lucene.orchestration.impl.LuceneQueryWorkOrchestrator;
import org.hibernate.search.backend.lucene.search.extraction.impl.LuceneCollectorProvider;
import org.hibernate.search.backend.lucene.work.impl.LuceneQueryWork;
import org.hibernate.search.backend.lucene.work.impl.LuceneWorkFactory;
import org.hibernate.search.engine.mapper.session.context.spi.SessionContextImplementor;
import org.hibernate.search.engine.search.query.spi.IndexSearchQuery;
import org.hibernate.search.engine.search.query.spi.IndexSearchResult;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;


/**
 * @author Guillaume Smet
 */
public class LuceneIndexSearchQuery<T> implements IndexSearchQuery<T> {

	private final LuceneQueryWorkOrchestrator queryOrchestrator;
	private final LuceneWorkFactory workFactory;
	private final Set<String> indexNames;
	private final Set<ReaderProvider> readerProviders;
	private final SessionContextImplementor sessionContext;
	private final Query luceneQuery;
	private final Sort luceneSort;
	private final LuceneCollectorProvider luceneCollectorProvider;
	private final LuceneSearchResultExtractor<T> searchResultExtractor;

	private Long firstResultIndex = 0L;
	private Long maxResultsCount;

	public LuceneIndexSearchQuery(LuceneQueryWorkOrchestrator queryOrchestrator,
			LuceneWorkFactory workFactory, Set<String> indexNames, Set<ReaderProvider> readerProviders,
			SessionContextImplementor sessionContext,
			Query luceneQuery, Sort luceneSort,
			LuceneCollectorProvider luceneCollectorProvider, LuceneSearchResultExtractor<T> searchResultExtractor) {
		this.queryOrchestrator = queryOrchestrator;
		this.workFactory = workFactory;
		this.indexNames = indexNames;
		this.readerProviders = readerProviders;
		this.sessionContext = sessionContext;
		this.luceneQuery = luceneQuery;
		this.luceneSort = luceneSort;
		this.luceneCollectorProvider = luceneCollectorProvider;
		this.searchResultExtractor = searchResultExtractor;
	}

	@Override
	public void setFirstResult(Long firstResultIndex) {
		this.firstResultIndex = firstResultIndex;
	}

	@Override
	public void setMaxResults(Long maxResultsCount) {
		this.maxResultsCount = maxResultsCount;
	}

	@Override
	public String getQueryString() {
		return luceneQuery.toString();
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[query=" + getQueryString() + ", sort=" + luceneSort + "]";
	}

	@Override
	public IndexSearchResult<T> execute() {
		LuceneQueryWork<LuceneLoadableSearchResult<T>> work = workFactory.search(
				new LuceneSearcher<>(
						indexNames,
						readerProviders,
						luceneQuery, luceneSort,
						firstResultIndex, maxResultsCount,
						luceneCollectorProvider, searchResultExtractor
				)
		);
		return queryOrchestrator.submit( work ).join()
				/*
				 * WARNING: the following call must run in the user thread.
				 * If we introduce async processing, we will have to add a loadAsync method here,
				 * as well as in ProjectionHitMapper and ObjectLoader.
				 * This method may not be easy to implement for blocking mappers,
				 * so we may choose to throw exceptions for those.
				 */
				.loadBlocking( sessionContext );
	}

	@Override
	public long executeCount() {
		LuceneQueryWork<LuceneLoadableSearchResult<T>> work = workFactory.search(
				new LuceneSearcher<>(
						indexNames,
						readerProviders,
						luceneQuery, luceneSort,
						0L, 0L,
						// do not add any TopDocs collector
						( luceneCollectorBuilder -> { } ),
						searchResultExtractor
				)
		);
		return queryOrchestrator.submit( work ).join().getHitCount();
	}
}
