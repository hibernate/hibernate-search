/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.query.impl;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.lucene.search.Sort;
import org.hibernate.search.elasticsearch.client.impl.URLEncodedString;
import org.hibernate.search.elasticsearch.impl.ElasticsearchQueryOptions;
import org.hibernate.search.elasticsearch.impl.ElasticsearchService;
import org.hibernate.search.elasticsearch.impl.JsonBuilder;
import org.hibernate.search.elasticsearch.impl.ToElasticsearch;
import org.hibernate.search.elasticsearch.work.impl.ElasticsearchWork;
import org.hibernate.search.elasticsearch.work.impl.ExplainResult;
import org.hibernate.search.elasticsearch.work.impl.SearchResult;
import org.hibernate.search.elasticsearch.work.impl.builder.SearchWorkBuilder;
import org.hibernate.search.engine.metadata.impl.FacetMetadata;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.query.engine.spi.EntityInfo;
import org.hibernate.search.query.facet.FacetingRequest;

import com.google.gson.JsonObject;

/**
 * Stores all information required to execute the query: query string, relevant indices, query parameters, ...
 */
class IndexSearcher {

	/**
	 * ES default limit for (firstResult + maxResult)
	 */
	private static final int MAX_RESULT_WINDOW_SIZE = 10000;

	final ElasticsearchService elasticsearchService;
	private final Set<URLEncodedString> indexNames;
	private final Map<FacetingRequest, FacetMetadata> facetingRequestsAndMetadata;

	private final ElasticsearchQueryOptions queryOptions;
	private final QueryHitConverter queryHitConverter;
	private final JsonObject payload;

	public IndexSearcher(ElasticsearchService elasticsearchService,
			Map<String, EntityIndexBinding> targetedEntityBindingsByName, Set<URLEncodedString> indexNames,
			JsonObject filteredQuery, QueryHitConverter queryHitConverter, Sort sort,
			Map<FacetingRequest, FacetMetadata> facetingRequestsAndMetadata) {
		this.elasticsearchService = elasticsearchService;
		this.indexNames = indexNames;
		this.facetingRequestsAndMetadata = facetingRequestsAndMetadata;
		this.queryOptions = elasticsearchService.getQueryOptions();
		this.queryHitConverter = queryHitConverter;

		JsonBuilder.Object payloadBuilder = JsonBuilder.object();
		payloadBuilder.add( "query", filteredQuery );

		queryHitConverter.contributeToPayload( payloadBuilder );

		if ( !facetingRequestsAndMetadata.isEmpty() ) {
			JsonBuilder.Object facets = JsonBuilder.object();
			for ( Entry<FacetingRequest, FacetMetadata> facetingRequestEntry : facetingRequestsAndMetadata.entrySet() ) {
				addFacetingRequest( facets, facetingRequestEntry.getKey(), facetingRequestEntry.getValue() );
			}
			payloadBuilder.add( "aggregations", facets );
		}

		// TODO: HSEARCH-2254 embedded fields (see https://github.com/searchbox-io/Jest/issues/304)
		if ( sort != null ) {
			payloadBuilder.add( "sort", ToElasticsearch.fromLuceneSort( sort ) );
		}

		this.payload = payloadBuilder.build();
	}

	public SearchResult search(int firstResult, Integer maxResults) {
		SearchWorkBuilder builder = elasticsearchService.getWorkFactory()
				.search( payload ).indexes( indexNames );

		builder.paging(
				firstResult,
				/*
				 * If the user has given a 'size' value, take it as is, let ES itself complain if it's too high;
				 * if no value is given, I take as much as possible, as by default only 10 rows would be returned
				 */
				maxResults != null ? maxResults : MAX_RESULT_WINDOW_SIZE - firstResult
		);

		ElasticsearchWork<SearchResult> work = builder.build();
		return elasticsearchService.getWorkProcessor().executeSyncUnsafe( work );
	}

	public SearchResult searchWithScrollEnabled() {
		SearchWorkBuilder builder = elasticsearchService.getWorkFactory()
				.search( payload ).indexes( indexNames );

		/*
		 * Note: "firstResult" is currently being ignored by Elasticsearch when scrolling.
		 * See https://github.com/elastic/elasticsearch/issues/9373
		 * To work this around, we don't use the "from" parameter here, and the document
		 * extractor will skip the results by scrolling until it gets the right index.
		 */
		builder.scrolling(
				/*
				 * The "size" parameter has a slightly different meaning when scrolling: it defines the window
				 * size for the first search *and* for the next calls to the scroll API.
				 * We still reduce the number of results in accordance to "maxResults", but that's done on our side
				 * in the document extractor.
				 */
				getQueryOptions().getScrollFetchSize(),
				getQueryOptions().getScrollTimeout()
		);

		ElasticsearchWork<SearchResult> work = builder.build();
		return elasticsearchService.getWorkProcessor().executeSyncUnsafe( work );
	}

	/**
	 * Scroll through search results, using a previously obtained scrollId.
	 */
	public SearchResult scroll(String scrollId) {
		ElasticsearchQueryOptions queryOptions = getQueryOptions();
		ElasticsearchWork<SearchResult> work = elasticsearchService.getWorkFactory()
				.scroll( scrollId, queryOptions.getScrollTimeout() ).build();
		return elasticsearchService.getWorkProcessor().executeSyncUnsafe( work );
	}

	public void clearScroll(String scrollId) {
		ElasticsearchWork<?> work = elasticsearchService.getWorkFactory()
				.clearScroll( scrollId ).build();
		elasticsearchService.getWorkProcessor().executeSyncUnsafe( work );
	}

	public EntityInfo convertQueryHit(SearchResult searchResult, JsonObject hit) {
		return this.queryHitConverter.convert( searchResult, hit );
	}

	public ExplainResult explain(JsonObject hit) {
		/*
		 * Do not add every property of the original payload: some properties, such as "_source", do not have the same syntax
		 * and are not necessary to the explanation.
		 */
		JsonObject explainPayload = JsonBuilder.object()
				.add( "query", payload.get( "query" ) )
				.build();

		ElasticsearchWork<ExplainResult> work = elasticsearchService.getWorkFactory().explain(
				URLEncodedString.fromJSon( hit.get( "_index" ) ),
				URLEncodedString.fromJSon( hit.get( "_type" ) ),
				URLEncodedString.fromJSon( hit.get( "_id" ) ),
				explainPayload )
				.build();

		return elasticsearchService.getWorkProcessor().executeSyncUnsafe( work );
	}

	public ElasticsearchQueryOptions getQueryOptions() {
		return queryOptions;
	}

	public Map<FacetingRequest, FacetMetadata> getFacetingRequestsAndMetadata() {
		return facetingRequestsAndMetadata;
	}

	private void addFacetingRequest(JsonBuilder.Object facets, FacetingRequest facetingRequest, FacetMetadata facetMetadata) {
		String sourceFieldAbsoluteName = facetMetadata.getSourceField().getAbsoluteName();
		String facetSubfieldName = facetMetadata.getPath().getRelativeName();

		ToElasticsearch.addFacetingRequest( facets, facetingRequest, sourceFieldAbsoluteName, facetSubfieldName );
	}

}