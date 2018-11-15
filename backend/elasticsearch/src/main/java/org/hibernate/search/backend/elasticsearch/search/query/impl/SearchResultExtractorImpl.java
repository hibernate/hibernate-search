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
import org.hibernate.search.backend.elasticsearch.search.projection.impl.SearchProjectionExecutionContext;
import org.hibernate.search.backend.elasticsearch.work.impl.SearchResultExtractor;
import org.hibernate.search.engine.search.SearchResult;
import org.hibernate.search.engine.search.query.spi.LoadingResult;
import org.hibernate.search.engine.search.query.spi.ProjectionHitMapper;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class SearchResultExtractorImpl<T> implements SearchResultExtractor<T> {

	private static final JsonObjectAccessor HITS_ACCESSOR =
			JsonAccessor.root().property( "hits" ).asObject();

	private static final JsonAccessor<JsonArray> HITS_HITS_ACCESSOR =
			HITS_ACCESSOR.property( "hits" ).asArray();

	private static final JsonAccessor<Long> HITS_TOTAL_ACCESSOR =
			HITS_ACCESSOR.property( "total" ).asLong();

	private final ProjectionHitMapper<?, ?> projectionHitMapper;
	private final ElasticsearchSearchProjection<T> rootProjection;

	private final SearchProjectionExecutionContext searchProjectionExecutionContext;

	public SearchResultExtractorImpl(
			ProjectionHitMapper<?, ?> projectionHitMapper,
			ElasticsearchSearchProjection<T> rootProjection,
			SearchProjectionExecutionContext searchProjectionExecutionContext) {
		this.projectionHitMapper = projectionHitMapper;
		this.rootProjection = rootProjection;
		this.searchProjectionExecutionContext = searchProjectionExecutionContext;
	}

	@Override
	public SearchResult<T> extract(JsonObject responseBody) {
		Long hitCount = HITS_TOTAL_ACCESSOR.get( responseBody ).orElse( 0L );

		final List<T> finalHits = hitCount > 0 ? extractHits( responseBody ) : Collections.emptyList();

		return new SearchResult<T>() {
			@Override
			public long getHitCount() {
				return hitCount;
			}

			@Override
			public List<T> getHits() {
				return finalHits;
			}
		};
	}

	@SuppressWarnings("unchecked")
	private List<T> extractHits(JsonObject responseBody) {
		JsonArray jsonHits = HITS_HITS_ACCESSOR.get( responseBody ).orElseGet( JsonArray::new );

		List<Object> hits = new ArrayList<>( jsonHits.size() );

		for ( JsonElement hit : jsonHits ) {
			JsonObject hitObject = hit.getAsJsonObject();

			hits.add( rootProjection.extract( projectionHitMapper, responseBody, hitObject,
					searchProjectionExecutionContext ) );
		}

		LoadingResult<?> loadingResult = projectionHitMapper.load();

		for ( int i = 0; i < hits.size(); i++ ) {
			hits.set( i, rootProjection.transform( loadingResult, hits.get( i ) ) );
		}

		return Collections.unmodifiableList( (List<T>) hits );
	}
}
