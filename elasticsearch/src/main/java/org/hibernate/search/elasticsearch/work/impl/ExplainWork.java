/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.work.impl;

import org.hibernate.search.elasticsearch.client.impl.ElasticsearchRequest;
import org.hibernate.search.elasticsearch.client.impl.ElasticsearchResponse;
import org.hibernate.search.elasticsearch.client.impl.Paths;
import org.hibernate.search.elasticsearch.client.impl.URLEncodedString;
import org.hibernate.search.elasticsearch.work.impl.builder.ExplainWorkBuilder;

import com.google.gson.JsonObject;

/**
 * @author Yoann Rodiere
 */
public class ExplainWork extends SimpleElasticsearchWork<ExplainResult> {

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
			extends SimpleElasticsearchWork.Builder<Builder>
			implements ExplainWorkBuilder {
		private final URLEncodedString indexName;
		private final URLEncodedString typeName;
		private final URLEncodedString id;
		private final JsonObject payload;

		public Builder(URLEncodedString indexName, URLEncodedString typeName, URLEncodedString id, JsonObject payload) {
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
					.pathComponent( typeName )
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