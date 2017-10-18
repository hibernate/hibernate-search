/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.impl;

import java.util.Map;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;

import com.google.gson.JsonObject;

class IndexSensitiveHitExtractor<T> implements HitExtractor<T> {
	private static final JsonAccessor<String> HIT_INDEX_NAME_ACCESSOR = JsonAccessor.root()
			.property( "_index" )
			.asString();

	private final Map<String, HitExtractor<T>> extractorByIndex;

	public IndexSensitiveHitExtractor(Map<String, HitExtractor<T>> extractorByIndex) {
		this.extractorByIndex = extractorByIndex;
	}

	@Override
	public void contributeRequest(JsonObject requestBody) {
		for ( HitExtractor<?> extractor : extractorByIndex.values() ) {
			extractor.contributeRequest( requestBody );
		}
	}

	@Override
	public T extractHit(JsonObject responseBody, JsonObject hit) {
		String indexName = HIT_INDEX_NAME_ACCESSOR.get( hit ).get();
		HitExtractor<T> delegate = extractorByIndex.get( indexName );
		return delegate.extractHit( responseBody, hit );
	}
}
