/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.query.impl;


import org.hibernate.search.backend.lucene.orchestration.impl.LuceneQueryWorkOrchestrator;
import org.hibernate.search.backend.lucene.search.extraction.impl.LuceneCollectorProvider;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.search.query.LuceneSearchQuery;
import org.hibernate.search.backend.lucene.search.query.LuceneSearchResult;
import org.hibernate.search.backend.lucene.work.impl.LuceneQueryWork;
import org.hibernate.search.backend.lucene.work.impl.LuceneWorkFactory;
import org.hibernate.search.engine.common.dsl.spi.DslExtensionState;
import org.hibernate.search.engine.mapper.session.context.spi.SessionContextImplementor;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContext;
import org.hibernate.search.engine.search.query.spi.AbstractSearchQuery;
import org.hibernate.search.engine.search.query.SearchQueryExtension;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;


/**
 * @author Guillaume Smet
 */
public class LuceneSearchQueryImpl<H> extends AbstractSearchQuery<H, LuceneSearchResult<H>>
		implements LuceneSearchQuery<H> {

	private final LuceneQueryWorkOrchestrator queryOrchestrator;
	private final LuceneWorkFactory workFactory;
	private final LuceneSearchContext searchContext;
	private final SessionContextImplementor sessionContext;
	private final LoadingContext<?, ?> loadingContext;
	private final Query luceneQuery;
	private final Sort luceneSort;
	private final LuceneCollectorProvider luceneCollectorProvider;
	private final LuceneSearchResultExtractor<H> searchResultExtractor;

	LuceneSearchQueryImpl(LuceneQueryWorkOrchestrator queryOrchestrator,
			LuceneWorkFactory workFactory, LuceneSearchContext searchContext,
			SessionContextImplementor sessionContext,
			LoadingContext<?, ?> loadingContext,
			Query luceneQuery, Sort luceneSort,
			LuceneCollectorProvider luceneCollectorProvider, LuceneSearchResultExtractor<H> searchResultExtractor) {
		this.queryOrchestrator = queryOrchestrator;
		this.workFactory = workFactory;
		this.searchContext = searchContext;
		this.sessionContext = sessionContext;
		this.loadingContext = loadingContext;
		this.luceneQuery = luceneQuery;
		this.luceneSort = luceneSort;
		this.luceneCollectorProvider = luceneCollectorProvider;
		this.searchResultExtractor = searchResultExtractor;
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
	public <Q> Q extension(SearchQueryExtension<Q, H> extension) {
		return DslExtensionState.returnIfSupported(
				extension, extension.extendOptional( this, loadingContext )
		);
	}

	@Override
	public LuceneSearchResult<H> fetch(Long limit, Long offset) {
		LuceneQueryWork<LuceneLoadableSearchResult<H>> work = workFactory.search(
				new LuceneSearcher<>(
						searchContext,
						luceneQuery, luceneSort,
						offset, limit,
						luceneCollectorProvider, searchResultExtractor
				)
		);
		return queryOrchestrator.submit( work ).join()
				/*
				 * WARNING: the following call must run in the user thread.
				 * If we introduce async processing, we will have to add a loadAsync method here,
				 * as well as in ProjectionHitMapper and EntityLoader.
				 * This method may not be easy to implement for blocking mappers,
				 * so we may choose to throw exceptions for those.
				 */
				.loadBlocking( sessionContext );
	}

	@Override
	public long fetchTotalHitCount() {
		LuceneQueryWork<LuceneLoadableSearchResult<H>> work = workFactory.search(
				new LuceneSearcher<>(
						searchContext,
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
