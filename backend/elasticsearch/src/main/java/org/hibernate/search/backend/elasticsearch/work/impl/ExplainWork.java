package org.hibernate.search.backend.elasticsearch.work.impl;

import java.util.Set;

import org.hibernate.search.backend.elasticsearch.client.common.impl.ElasticsearchJsonRequest;
import org.hibernate.search.backend.elasticsearch.client.common.impl.ElasticsearchJsonResponse;
import org.hibernate.search.backend.elasticsearch.client.common.util.spi.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.client.impl.Paths;
import org.hibernate.search.backend.elasticsearch.logging.impl.QueryLog;
import org.hibernate.search.backend.elasticsearch.work.result.impl.ExplainResult;

import tools.jackson.databind.node.ObjectNode;

public class ExplainWork extends AbstractNonBulkableWork<ExplainResult> {

	private static final ElasticsearchRequestSuccessAssessor SUCCESS_ASSESSOR =
			ElasticsearchRequestSuccessAssessor.builder().ignoreErrorStatuses( 404 ).build();

	private final URLEncodedString indexName;

	private final URLEncodedString id;

	private ExplainWork(Builder builder) {
		super( builder );
		this.indexName = builder.indexName;
		this.id = builder.id;
	}

	@Override
	protected ExplainResult generateResult(ElasticsearchWorkExecutionContext context, ElasticsearchJsonResponse response) {
		if ( response.statusCode() == 404 ) {
			throw QueryLog.INSTANCE.explainUnknownDocument( indexName, id );
		}
		ObjectNode body = response.bodyJson();
		return new ExplainResultImpl( body );
	}

	public static class Builder extends AbstractBuilder<Builder> {

		private final URLEncodedString indexName;

		private final URLEncodedString id;

		private final ObjectNode payload;

		private Set<String> routingKeys;

		public static Builder create(URLEncodedString indexName, URLEncodedString id, ObjectNode payload) {
			return new Builder( indexName, id, payload );
		}

		private Builder(URLEncodedString indexName, URLEncodedString id, ObjectNode payload) {
			super( SUCCESS_ASSESSOR );
			this.indexName = indexName;
			this.id = id;
			this.payload = payload;
		}

		public Builder routingKeys(Set<String> routingKeys) {
			this.routingKeys = routingKeys;
			return this;
		}

		@Override
		protected ElasticsearchJsonRequest buildRequest() {
			ElasticsearchJsonRequest.Builder builder = ElasticsearchJsonRequest.get().pathComponent( indexName )
					.pathComponent( Paths._EXPLAIN ).pathComponent( id ).bodyJsonFromInternal( payload );
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

		private final ObjectNode jsonObject;

		private ExplainResultImpl(ObjectNode jsonObject) {
			super();
			this.jsonObject = jsonObject;
		}

		@Override
		public ObjectNode getJsonObject() {
			return jsonObject;
		}
	}
}
