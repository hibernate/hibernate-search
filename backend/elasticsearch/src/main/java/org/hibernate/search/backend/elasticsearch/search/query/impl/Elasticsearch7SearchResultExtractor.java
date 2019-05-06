/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.query.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonObjectAccessor;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.ElasticsearchSearchProjection;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.SearchProjectionExtractContext;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchSearchResultExtractor;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

class Elasticsearch7SearchResultExtractor<T> implements ElasticsearchSearchResultExtractor<T> {

	protected static final JsonObjectAccessor HITS_ACCESSOR =
			JsonAccessor.root().property( "hits" ).asObject();

	private static final JsonAccessor<JsonArray> HITS_HITS_ACCESSOR =
			HITS_ACCESSOR.property( "hits" ).asArray();

	private static final JsonAccessor<Long> HITS_TOTAL_ACCESSOR =
			HITS_ACCESSOR.property( "total" ).property( "value" ).asLong();

	private final ProjectionHitMapper<?, ?> projectionHitMapper;
	private final ElasticsearchSearchProjection<?, T> rootProjection;

	private final SearchProjectionExtractContext searchProjectionExecutionContext;

	Elasticsearch7SearchResultExtractor(
			ProjectionHitMapper<?, ?> projectionHitMapper,
			ElasticsearchSearchProjection<?, T> rootProjection,
			SearchProjectionExtractContext searchProjectionExecutionContext) {
		this.projectionHitMapper = projectionHitMapper;
		this.rootProjection = rootProjection;
		this.searchProjectionExecutionContext = searchProjectionExecutionContext;
	}

	@Override
	public ElasticsearchLoadableSearchResult<T> extract(JsonObject responseBody) {
		long hitCount = extractHitCount( responseBody );

		final List<Object> extractedData = hitCount > 0 ? extractHits( responseBody ) : Collections.emptyList();

		return new ElasticsearchLoadableSearchResult<>( projectionHitMapper, rootProjection, hitCount, extractedData );
	}

	protected long extractHitCount(JsonObject responseBody) {
		return HITS_TOTAL_ACCESSOR.get( responseBody ).orElse( 0L );
	}

	private List<Object> extractHits(JsonObject responseBody) {
		JsonArray jsonHits = HITS_HITS_ACCESSOR.get( responseBody ).orElseGet( JsonArray::new );

		List<Object> extractedData = new ArrayList<>( jsonHits.size() );

		for ( JsonElement hit : jsonHits ) {
			JsonObject hitObject = hit.getAsJsonObject();

			extractedData.add( rootProjection.extract( projectionHitMapper, responseBody, hitObject,
					searchProjectionExecutionContext ) );
		}

		return extractedData;
	}
}
