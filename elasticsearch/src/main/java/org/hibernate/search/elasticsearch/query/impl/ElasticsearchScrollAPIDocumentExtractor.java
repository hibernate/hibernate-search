/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.query.impl;

import java.io.IOException;

import org.apache.lucene.search.TopDocs;
import org.hibernate.search.elasticsearch.impl.ElasticsearchQueryOptions;
import org.hibernate.search.elasticsearch.logging.impl.Log;
import org.hibernate.search.elasticsearch.util.impl.Window;
import org.hibernate.search.elasticsearch.work.impl.SearchResult;
import org.hibernate.search.query.engine.spi.DocumentExtractor;
import org.hibernate.search.query.engine.spi.EntityInfo;
import org.hibernate.search.util.logging.impl.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

class ElasticsearchScrollAPIDocumentExtractor implements DocumentExtractor {

	private static final Log LOG = LoggerFactory.make( Log.class );

	// Search parameters
	private final IndexSearcher searcher;
	private final int firstIndex;
	private final Integer queryIndexLimit;

	// Position
	private String scrollId;

	// Results
	private Integer totalResultCount;
	private final Window<EntityInfo> results;

	public ElasticsearchScrollAPIDocumentExtractor(IndexSearcher searcher, int firstResult, Integer maxResults) {
		this.searcher = searcher;
		this.firstIndex = firstResult;
		this.queryIndexLimit = maxResults == null
				? null : firstResult + maxResults;
		ElasticsearchQueryOptions queryOptions = searcher.getQueryOptions();
		this.results = new Window<>(
				/*
				 * The offset is currently ignored by Elasticsearch.
				 * See https://github.com/elastic/elasticsearch/issues/9373
				 * To work this around, we don't use the "from" parameter when querying, and the document
				 * extractor will skip the results by querying until it gets the right index.
				 */
				0,
				/*
				 * Sizing for the worst-case scenario: we just fetched a batch of elements to
				 * give access to the result just after the previously fetched results, and
				 * we still need to keep enough of the previous elements to backtrack.
				 */
				queryOptions.getScrollBacktrackingWindowSize() + queryOptions.getScrollFetchSize()
				);
	}

	@Override
	public EntityInfo extract(int index) throws IOException {
		if ( index < 0 ) {
			throw new IndexOutOfBoundsException( "Index must be >= 0" );
		}
		else if ( index < results.start() ) {
			throw LOG.backtrackingWindowOverflow( searcher.getQueryOptions().getScrollBacktrackingWindowSize(), results.start(), index );
		}

		if ( totalResultCount == null ) {
			initResults();
		}

		int maxIndex = getMaxIndex();
		if ( maxIndex < index ) {
			throw new IndexOutOfBoundsException( "Index must be <= " + maxIndex );
		}

		boolean fetchMayReturnResults = true;
		while ( results.start() + results.size() <= index && fetchMayReturnResults ) {
			fetchMayReturnResults = fetchNextResults();
		}

		return results.get( index );
	}

	@Override
	public int getFirstIndex() {
		return firstIndex;
	}

	@Override
	public int getMaxIndex() {
		if ( totalResultCount == null ) {
			initResults();
		}

		if ( queryIndexLimit == null ) {
			return totalResultCount - 1;
		}
		else {
			return Math.min( totalResultCount, queryIndexLimit ) - 1;
		}
	}

	@Override
	public void close() {
		if ( scrollId != null ) {
			searcher.clearScroll( scrollId );
			scrollId = null;
			totalResultCount = null;
			results.clear();
		}
	}

	@Override
	public TopDocs getTopDocs() {
		throw LOG.documentExtractorTopDocsUnsupported();
	}

	private void initResults() {
		SearchResult searchResult = searcher.searchWithScrollEnabled();
		totalResultCount = searchResult.getTotalHitCount();
		extractWindow( searchResult );
	}

	/**
	 * @return {@code true} if at least one result was fetched, {@code false} otherwise.
	 */
	private boolean fetchNextResults() {
		if ( totalResultCount <= results.start() + results.size() ) {
			// No more results to fetch
			return false;
		}

		SearchResult searchResult = searcher.scroll( scrollId );
		return extractWindow( searchResult );
	}

	/**
	 * @return {@code true} if at least one result was fetched, {@code false} otherwise.
	 */
	private boolean extractWindow(SearchResult searchResult) {
		boolean fetchedAtLeastOne = false;
		scrollId = searchResult.getScrollId();
		JsonArray hits = searchResult.getHits();
		for ( JsonElement hit : hits ) {
			EntityInfo converted = searcher.convertQueryHit( searchResult, hit.getAsJsonObject() );
			if ( converted != null ) {
				results.add( converted );
				fetchedAtLeastOne = true;
			}
		}
		return fetchedAtLeastOne;
	}
}