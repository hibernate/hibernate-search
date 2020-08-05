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
import org.hibernate.search.backend.elasticsearch.work.builder.factory.impl.ElasticsearchWorkBuilderFactory;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchSearchResultExtractor;
import org.hibernate.search.backend.elasticsearch.work.impl.NonBulkableWork;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.impl.Futures;

public class ElasticsearchSearchScrollImpl<H> implements ElasticsearchSearchScroll<H> {

	private final ElasticsearchParallelWorkOrchestrator queryOrchestrator;
	private final ElasticsearchWorkBuilderFactory workFactory;
	private final ElasticsearchSearchResultExtractor<ElasticsearchLoadableSearchResult<H>> searchResultExtractor;
	private final String scrollTimeoutString;
	private final NonBulkableWork<ElasticsearchLoadableSearchResult<H>> firstScroll;

	private String scrollId;

	public ElasticsearchSearchScrollImpl(ElasticsearchParallelWorkOrchestrator queryOrchestrator, ElasticsearchWorkBuilderFactory workFactory,
			ElasticsearchSearchResultExtractor<ElasticsearchLoadableSearchResult<H>> searchResultExtractor, String scrollTimeoutString,
			NonBulkableWork<ElasticsearchLoadableSearchResult<H>> firstScroll) {
		this.workFactory = workFactory;
		this.queryOrchestrator = queryOrchestrator;
		this.searchResultExtractor = searchResultExtractor;
		this.scrollTimeoutString = scrollTimeoutString;
		this.firstScroll = firstScroll;
	}

	@Override
	public void close() {
		if ( scrollId != null ) {
			Futures.unwrappedExceptionJoin( queryOrchestrator.submit( workFactory.clearScroll( scrollId ).build() ) );
		}
	}

	@Override
	public ElasticsearchSearchScrollResult<H> next() {
		NonBulkableWork<ElasticsearchLoadableSearchResult<H>> scroll = ( scrollId == null ) ? firstScroll :
				workFactory.scroll( scrollId, scrollTimeoutString, searchResultExtractor ).build();

		ElasticsearchLoadableSearchResult<H> loadableSearchResult = Futures.unwrappedExceptionJoin( queryOrchestrator.submit( scroll ) );
		ElasticsearchSearchResultImpl<H> searchResult = loadableSearchResult.loadBlocking();

		scrollId = searchResult.scrollId();
		if ( scrollId == null ) {
			throw new AssertionFailure( "Elasticsearch response lacked a value for scroll id" );
		}

		return new ElasticsearchSearchScrollResultImpl<>( loadableSearchResult.hasHits(), searchResult.hits(),
				searchResult.took(), searchResult.timedOut() );
	}
}
