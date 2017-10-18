/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.impl;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonObject;

class CompositeHitExtractor implements HitExtractor<List<?>> {

	private final List<HitExtractor<?>> extractors;

	public CompositeHitExtractor(List<HitExtractor<?>> extractors) {
		this.extractors = extractors;
	}

	public void contributeRequest(JsonObject requestBody) {
		for ( HitExtractor<?> extractor : extractors ) {
			extractor.contributeRequest( requestBody );
		}
	}

	public List<?> extractHit(JsonObject responseBody, JsonObject hit) {
		List<Object> result = new ArrayList<>( extractors.size() );
		for ( HitExtractor<?> extractor : extractors ) {
			Object element = extractor.extractHit( responseBody, hit );
			result.add( element );
		}
		return result;
	}

}
