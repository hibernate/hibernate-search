/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.extraction.impl;

import java.util.List;

import org.hibernate.search.backend.elasticsearch.search.projection.impl.SearchProjectionExecutionContext;

import com.google.gson.JsonObject;

/**
 * A hit extractor used when multiple values must be extracted for each hit.
 * <p>
 * Used for projections.
 */
public class CompositeHitExtractor<C> implements HitExtractor<C> {

	private final List<HitExtractor<? super C>> extractors;

	public CompositeHitExtractor(List<HitExtractor<? super C>> extractors) {
		this.extractors = extractors;
	}

	@Override
	public void contributeRequest(JsonObject requestBody, SearchProjectionExecutionContext searchProjectionExecutionContext) {
		for ( HitExtractor<?> extractor : extractors ) {
			extractor.contributeRequest( requestBody, searchProjectionExecutionContext );
		}
	}

	@Override
	public void extract(C collector, JsonObject responseBody, JsonObject hit, SearchProjectionExecutionContext searchProjectionExecutionContext) {
		for ( HitExtractor<? super C> extractor : extractors ) {
			extractor.extract( collector, responseBody, hit, searchProjectionExecutionContext );
		}
	}

}
