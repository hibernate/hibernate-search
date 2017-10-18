/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.work.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.search.impl.HitExtractor;
import org.hibernate.search.engine.search.SearchResult;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;


/**
 * @author Yoann Rodiere
 */
public class StubElasticsearchWorkFactory implements ElasticsearchWorkFactory {

	private static final JsonAccessor<JsonArray> HITS_HITS_ACCESSOR = JsonAccessor.root().property( "hits" ).property( "hits" ).asArray();

	@Override
	public ElasticsearchWork<?> createIndex(String indexName, JsonObject model) {
		return new StubElasticsearchWork<>( "createIndex", model )
				.addParam( "indexName", indexName );
	}

	@Override
	public ElasticsearchWork<?> add(String indexName, String id, JsonObject document) {
		return new StubElasticsearchWork<>( "add", document )
				.addParam( "indexName", indexName )
				.addParam( "id", id );
	}

	@Override
	public ElasticsearchWork<?> update(String indexName, String id, JsonObject document) {
		return new StubElasticsearchWork<>( "update", document )
				.addParam( "indexName", indexName )
				.addParam( "id", id );
	}

	@Override
	public ElasticsearchWork<?> delete(String indexName, String id) {
		return new StubElasticsearchWork<>( "delete", null )
				.addParam( "indexName", indexName )
				.addParam( "id", id );
	}

	@Override
	public ElasticsearchWork<?> flush(String indexName) {
		return new StubElasticsearchWork<>( "flush", null )
				.addParam( "indexName", indexName );
	}

	@Override
	public ElasticsearchWork<?> optimize(String indexName) {
		return new StubElasticsearchWork<>( "optimize", null )
				.addParam( "indexName", indexName );
	}

	@Override
	public <T> ElasticsearchWork<SearchResult<T>> search(Set<String> indexNames, JsonObject payload,
			HitExtractor<T> hitExtractor, Long offset, Long limit) {
		return new StubElasticsearchWork<SearchResult<T>>( "search", payload )
				.addParam( "indexName", indexNames )
				.addParam( "offset", offset, String::valueOf )
				.addParam( "limit", limit, String::valueOf )
				.setResultFunction( (object) -> generateSearchResult( hitExtractor, object ) );
	}

	private static <T> SearchResult<T> generateSearchResult(HitExtractor<T> hitExtractor, JsonObject jsonObject) {
		List<T> hits = new ArrayList<>();
		Optional<JsonArray> jsonHits = HITS_HITS_ACCESSOR.get( jsonObject );
		for ( JsonElement hit : jsonHits.orElseGet( JsonArray::new ) ) {
			JsonObject hitObject = hit.getAsJsonObject();
			hits.add( hitExtractor.extractHit( jsonObject, hitObject ) );
		}
		final List<T> finalHits = Collections.unmodifiableList( hits );
		return new SearchResult<T>() {
			@Override
			public long getHitCount() {
				return finalHits.size();
			}

			@Override
			public List<T> getHits() {
				return finalHits;
			}
		};
	}

}
