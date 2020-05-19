/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.work.impl;

import java.lang.invoke.MethodHandles;
import java.util.Set;

import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchRequest;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchResponse;
import org.hibernate.search.backend.elasticsearch.client.impl.Paths;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.work.builder.impl.ExplainWorkBuilder;
import org.hibernate.search.backend.elasticsearch.work.result.impl.ExplainResult;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import com.google.gson.JsonObject;


public class ExplainWork extends AbstractNonBulkableWork<ExplainResult> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final ElasticsearchRequestSuccessAssessor SUCCESS_ASSESSOR =
			DefaultElasticsearchRequestSuccessAssessor.builder().ignoreErrorStatuses( 404 ).build();

	private final URLEncodedString id;

	private ExplainWork(Builder builder) {
		super( builder );
		this.id = builder.id;
	}

	@Override
	protected ExplainResult generateResult(ElasticsearchWorkExecutionContext context,
			ElasticsearchResponse response) {
		if ( response.statusCode() == 404 ) {
			throw log.explainUnknownDocument( id );
		}
		JsonObject body = response.body();
		return new ExplainResultImpl( body );
	}

	public static class Builder
			extends AbstractBuilder<Builder>
			implements ExplainWorkBuilder {
		private final URLEncodedString indexName;
		private final URLEncodedString typeName;
		private final URLEncodedString id;
		private final JsonObject payload;

		private Set<String> routingKeys;

		public static Builder forElasticsearch67AndBelow(URLEncodedString indexName, URLEncodedString typeName,
				URLEncodedString id, JsonObject payload) {
			return new Builder( indexName, typeName, id, payload );
		}

		public static Builder forElasticsearch7AndAbove(URLEncodedString indexName,
				URLEncodedString id, JsonObject payload) {
			return new Builder( indexName, null, id, payload );
		}

		private Builder(URLEncodedString indexName, URLEncodedString typeName, URLEncodedString id, JsonObject payload) {
			super( SUCCESS_ASSESSOR );
			this.indexName = indexName;
			this.typeName = typeName;
			this.id = id;
			this.payload = payload;
		}

		@Override
		public Builder routingKeys(Set<String> routingKeys) {
			this.routingKeys = routingKeys;
			return this;
		}

		@Override
		protected ElasticsearchRequest buildRequest() {
			ElasticsearchRequest.Builder builder =
					ElasticsearchRequest.get()
					.pathComponent( indexName );
			if ( typeName != null ) { // ES6.x and below only
				builder.pathComponent( typeName )
						.pathComponent( id )
						.pathComponent( Paths._EXPLAIN );
			}
			else { // ES7.x and above
				builder.pathComponent( Paths._EXPLAIN )
						.pathComponent( id );
			}
			builder.body( payload );

			if ( !routingKeys.isEmpty() ) {
				builder.multiValuedParam( "routing", routingKeys );
			}

			return builder.build();
		}

		@Override
		public ExplainWork build() {
			return new ExplainWork( this );
		}
	}

	private static class ExplainResultImpl implements ExplainResult {

		private final JsonObject jsonObject;

		private ExplainResultImpl(JsonObject jsonObject) {
			super();
			this.jsonObject = jsonObject;
		}

		@Override
		public JsonObject getJsonObject() {
			return jsonObject;
		}
	}
}