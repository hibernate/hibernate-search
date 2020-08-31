/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.query.impl;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.hibernate.search.backend.elasticsearch.search.query.ElasticsearchSearchResult;
import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.engine.search.query.SearchResultTotal;
import org.hibernate.search.engine.search.query.spi.SimpleSearchResult;

import com.google.gson.JsonObject;

class ElasticsearchSearchResultImpl<H> extends SimpleSearchResult<H>
		implements ElasticsearchSearchResult<H> {

	private final JsonObject responseBody;
	private final String scrollId;

	ElasticsearchSearchResultImpl(JsonObject responseBody, SearchResultTotal resultTotal, List<H> hits,
			Map<AggregationKey<?>, ?> aggregationResults, Integer took, Boolean timedOut, String scrollId) {
		super( resultTotal, hits, aggregationResults, ( took == null ) ? null : Duration.ofMillis( took ), timedOut );
		this.responseBody = responseBody;
		this.scrollId = scrollId;
	}

	@Override
	public JsonObject responseBody() {
		return responseBody;
	}

	public String scrollId() {
		return scrollId;
	}
}
