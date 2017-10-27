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

class IndexSensitiveHitExtractor<C> implements HitExtractor<C> {
	private static final JsonAccessor<String> HIT_INDEX_NAME_ACCESSOR = JsonAccessor.root()
			.property( "_index" )
			.asString();

	private final Map<String, HitExtractor<? super C>> extractorByIndex;

	public IndexSensitiveHitExtractor(Map<String, HitExtractor<? super C>> extractorByIndex) {
		this.extractorByIndex = extractorByIndex;
	}

	@Override
	public void contributeRequest(JsonObject requestBody) {
		for ( HitExtractor<?> extractor : extractorByIndex.values() ) {
			extractor.contributeRequest( requestBody );
		}
	}

	@Override
	public void extract(C collector, JsonObject responseBody, JsonObject hit) {
		String indexName = HIT_INDEX_NAME_ACCESSOR.get( hit ).get();
		HitExtractor<? super C> delegate = extractorByIndex.get( indexName );
		delegate.extract( collector, responseBody, hit );
	}
}
