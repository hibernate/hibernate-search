/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.aggregation.impl;

import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;

import com.google.gson.JsonObject;

class ElasticsearchUserProvidedJsonAggregation extends AbstractElasticsearchAggregation<JsonObject> {

	private final JsonObject requestJson;

	private ElasticsearchUserProvidedJsonAggregation(Builder builder) {
		super( builder );
		this.requestJson = builder.json;
	}

	@Override
	public JsonObject request(AggregationRequestContext context) {
		return requestJson;
	}

	@Override
	public JsonObject extract(JsonObject aggregationResult, AggregationExtractContext context) {
		return aggregationResult;
	}

	static class Builder extends AbstractBuilder<JsonObject> {

		private final JsonObject json;

		Builder(ElasticsearchSearchIndexScope<?> scope, JsonObject json) {
			super( scope );
			this.json = json;
		}

		@Override
		public ElasticsearchSearchAggregation<JsonObject> build() {
			return new ElasticsearchUserProvidedJsonAggregation( this );
		}
	}
}
