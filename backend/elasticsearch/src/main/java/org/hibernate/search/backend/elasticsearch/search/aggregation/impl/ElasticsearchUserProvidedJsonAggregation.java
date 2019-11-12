/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.aggregation.impl;

import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchContext;

import com.google.gson.JsonObject;


class ElasticsearchUserProvidedJsonAggregation extends AbstractElasticsearchAggregation<String> {

	private final Gson gson;
	private final JsonObject requestJson;

	private ElasticsearchUserProvidedJsonAggregation(Builder builder) {
		super( builder );
		this.gson = builder.searchContext.getUserFacingGson();
		this.requestJson = builder.json;
	}

	@Override
	public JsonObject request(AggregationRequestContext context) {
		return requestJson;
	}

	@Override
	public String extract(JsonObject aggregationResult, AggregationExtractContext context) {
		return gson.toJson( aggregationResult );
	}

	static class Builder extends AbstractBuilder<String> {

		private final JsonObject json;

		Builder(ElasticsearchSearchContext searchContext, JsonObject json) {
			super( searchContext );
			this.json = json;
		}

		@Override
		public ElasticsearchSearchAggregation<String> build() {
			return new ElasticsearchUserProvidedJsonAggregation( this );
		}
	}
}
