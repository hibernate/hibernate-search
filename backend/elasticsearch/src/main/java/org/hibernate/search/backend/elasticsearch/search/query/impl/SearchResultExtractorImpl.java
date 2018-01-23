/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.query.impl;

import java.util.Collections;
import java.util.List;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonObjectAccessor;
import org.hibernate.search.engine.search.SearchResult;
import org.hibernate.search.engine.search.query.spi.HitAggregator;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class SearchResultExtractorImpl<C, T> implements SearchResultExtractor<T> {

	private static final JsonObjectAccessor HITS_ACCESSOR =
			JsonAccessor.root().property( "hits" ).asObject();

	private static final JsonAccessor<JsonArray> HITS_HITS_ACCESSOR =
			HITS_ACCESSOR.property( "hits" ).asArray();

	private static final JsonAccessor<Long> HITS_TOTAL_ACCESSOR =
			HITS_ACCESSOR.property( "total" ).asLong();

	private final HitExtractor<? super C> hitExtractor;
	private final HitAggregator<C, List<T>> hitAggregator;

	public SearchResultExtractorImpl(
			HitExtractor<? super C> hitExtractor,
			HitAggregator<C, List<T>> hitAggregator) {
		this.hitExtractor = hitExtractor;
		this.hitAggregator = hitAggregator;
	}

	@Override
	public SearchResult<T> extract(JsonObject responseBody) {
		Long hitCount = HITS_TOTAL_ACCESSOR.get( responseBody ).orElse( 0L );

		JsonArray jsonHits = HITS_HITS_ACCESSOR.get( responseBody ).orElseGet( JsonArray::new );

		hitAggregator.init( jsonHits.size() );

		for ( JsonElement hit : jsonHits ) {
			JsonObject hitObject = hit.getAsJsonObject();
			C hitCollector = hitAggregator.nextCollector();
			hitExtractor.extract( hitCollector, responseBody, hitObject );
		}

		final List<T> finalHits = Collections.unmodifiableList( hitAggregator.build() );
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

}
