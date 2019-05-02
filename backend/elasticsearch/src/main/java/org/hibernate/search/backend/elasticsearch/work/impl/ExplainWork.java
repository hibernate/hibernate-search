/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.work.impl;

import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchRequest;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchResponse;
import org.hibernate.search.backend.elasticsearch.client.impl.Paths;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.work.builder.impl.ExplainWorkBuilder;
import org.hibernate.search.backend.elasticsearch.work.result.impl.ExplainResult;

import com.google.gson.JsonObject;

/**
 * @author Yoann Rodiere
 */
public class ExplainWork extends AbstractSimpleElasticsearchWork<ExplainResult> {

	protected ExplainWork(Builder builder) {
		super( builder );
	}

	@Override
	protected ExplainResult generateResult(ElasticsearchWorkExecutionContext context,
			ElasticsearchResponse response) {
		JsonObject body = response.getBody();
		return new ExplainResultImpl( body );
	}

	public static class Builder
			extends AbstractBuilder<Builder>
			implements ExplainWorkBuilder {
		private final URLEncodedString indexName;
		private final URLEncodedString typeName;
		private final URLEncodedString id;
		private final JsonObject payload;

		public static Builder forElasticsearch67AndBelow(URLEncodedString indexName, URLEncodedString typeName,
				URLEncodedString id, JsonObject payload) {
			return new Builder( indexName, typeName, id, payload );
		}

		public static Builder forElasticsearch7AndAbove(URLEncodedString indexName,
				URLEncodedString id, JsonObject payload) {
			return new Builder( indexName, null, id, payload );
		}

		private Builder(URLEncodedString indexName, URLEncodedString typeName, URLEncodedString id, JsonObject payload) {
			super( null, DefaultElasticsearchRequestSuccessAssessor.INSTANCE );
			this.indexName = indexName;
			this.typeName = typeName;
			this.id = id;
			this.payload = payload;
		}

		@Override
		protected ElasticsearchRequest buildRequest() {
			ElasticsearchRequest.Builder builder =
					ElasticsearchRequest.get()
					.pathComponent( indexName )
					.pathComponent( typeName != null ? typeName : Paths._DOC ) // _doc for ES7+
					.pathComponent( id )
					.pathComponent( Paths._EXPLAIN )
					.body( payload );
			return builder.build();
		}

		@Override
		public ExplainWork build() {
			return new ExplainWork( this );
		}
	}

	private static class ExplainResultImpl implements ExplainResult {

		private final JsonObject jsonObject;

		public ExplainResultImpl(JsonObject jsonObject) {
			super();
			this.jsonObject = jsonObject;
		}

		@Override
		public JsonObject getJsonObject() {
			return jsonObject;
		}
	}
}