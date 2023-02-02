/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.query.impl;

import org.hibernate.search.backend.elasticsearch.orchestration.impl.ElasticsearchParallelWorkOrchestrator;
import org.hibernate.search.backend.elasticsearch.search.query.ElasticsearchSearchScroll;
import org.hibernate.search.backend.elasticsearch.search.query.ElasticsearchSearchScrollResult;
import org.hibernate.search.backend.elasticsearch.work.factory.impl.ElasticsearchWorkFactory;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchSearchResultExtractor;
import org.hibernate.search.backend.elasticsearch.work.impl.NonBulkableWork;
import org.hibernate.search.backend.elasticsearch.work.impl.SearchWork;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.engine.search.timeout.spi.TimeoutManager;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.impl.Futures;

public class ElasticsearchSearchScrollImpl<H> implements ElasticsearchSearchScroll<H> {

	private final ElasticsearchParallelWorkOrchestrator queryOrchestrator;
	private final ElasticsearchWorkFactory workFactory;
	private final ElasticsearchSearchResultExtractor<ElasticsearchLoadableSearchResult<H>> searchResultExtractor;
	private final String scrollTimeoutString;
	private final SearchWork.Builder<ElasticsearchLoadableSearchResult<H>> firstScroll;
	private final TimeoutManager timeoutManager;

	private String scrollId;

	public ElasticsearchSearchScrollImpl(ElasticsearchParallelWorkOrchestrator queryOrchestrator,
			ElasticsearchWorkFactory workFactory,
			ElasticsearchSearchResultExtractor<ElasticsearchLoadableSearchResult<H>> searchResultExtractor,
			String scrollTimeoutString,
			SearchWork.Builder<ElasticsearchLoadableSearchResult<H>> firstScroll,
			TimeoutManager timeoutManager) {
		this.workFactory = workFactory;
		this.queryOrchestrator = queryOrchestrator;
		this.searchResultExtractor = searchResultExtractor;
		this.scrollTimeoutString = scrollTimeoutString;
		this.firstScroll = firstScroll;
		this.timeoutManager = timeoutManager;
	}

	@Override
	public void close() {
		if ( scrollId != null ) {
			Futures.unwrappedExceptionJoin(
					queryOrchestrator.submit(
							workFactory.clearScroll( scrollId ).build(),
							OperationSubmitter.blocking()
					)
			);
		}
	}

	@Override
	public ElasticsearchSearchScrollResult<H> next() {
		timeoutManager.start();

		NonBulkableWork<ElasticsearchLoadableSearchResult<H>> scroll = ( scrollId == null ) ? firstScroll.build() :
				workFactory.scroll( scrollId, scrollTimeoutString, searchResultExtractor )
						.deadline( timeoutManager.deadlineOrNull(), timeoutManager.hasHardTimeout() )
						.build();

		ElasticsearchLoadableSearchResult<H> loadableSearchResult = Futures.unwrappedExceptionJoin(
				queryOrchestrator.submit(
						scroll,
						OperationSubmitter.blocking()
				)
		);
		ElasticsearchSearchResultImpl<H> searchResult = loadableSearchResult.loadBlocking();

		scrollId = searchResult.scrollId();
		if ( scrollId == null ) {
			throw new AssertionFailure( "Elasticsearch response lacked a value for scroll id" );
		}

		timeoutManager.stop();

		return new ElasticsearchSearchScrollResultImpl<>( searchResult.total(), loadableSearchResult.hasHits(),
				searchResult.hits(), searchResult.took(), searchResult.timedOut() );
	}
}
