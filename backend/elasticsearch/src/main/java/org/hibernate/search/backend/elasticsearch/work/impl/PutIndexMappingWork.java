package org.hibernate.search.backend.elasticsearch.work.impl;

import org.hibernate.search.backend.elasticsearch.client.common.impl.ElasticsearchJsonRequest;
import org.hibernate.search.backend.elasticsearch.client.common.impl.ElasticsearchJsonResponse;
import org.hibernate.search.backend.elasticsearch.client.common.json.internal.spi.JsonProvider;
import org.hibernate.search.backend.elasticsearch.client.common.util.spi.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.client.impl.Paths;
import org.hibernate.search.backend.elasticsearch.json.internal.spi.JsonCodecService;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.RootTypeMapping;

import tools.jackson.databind.node.ObjectNode;

public class PutIndexMappingWork extends AbstractNonBulkableWork<Void> {

	protected PutIndexMappingWork(Builder builder) {
		super( builder );
	}

	@Override
	protected Void generateResult(ElasticsearchWorkExecutionContext context, ElasticsearchJsonResponse response) {
		return null;
	}

	public static class Builder extends AbstractBuilder<Builder> {

		private final URLEncodedString indexName;

		private final ObjectNode payload;

		public static Builder create(JsonProvider jsonProvider, URLEncodedString indexName, RootTypeMapping typeMapping) {
			return new Builder( jsonProvider, indexName, typeMapping );
		}

		private Builder(JsonProvider jsonProvider, URLEncodedString indexName, RootTypeMapping typeMapping) {
			super( ElasticsearchRequestSuccessAssessor.DEFAULT_INSTANCE );
			this.indexName = indexName;
			/*
			 * Serializing nulls is really not a good idea here, it triggers NPEs in Elasticsearch
			 * We better not include the null fields.
			 */
			JsonCodecService json = jsonProvider.getCodecNoSerializeNulls();
			this.payload = ( (ObjectNode) json.encode( typeMapping ) );
		}

		@Override
		protected ElasticsearchJsonRequest buildRequest() {
			ElasticsearchJsonRequest.Builder builder = ElasticsearchJsonRequest.put().pathComponent( indexName );
			builder.pathComponent( Paths._MAPPING ).bodyJsonFromInternal( payload );
			return builder.build();
		}

		@Override
		public PutIndexMappingWork build() {
			return new PutIndexMappingWork( this );
		}
	}
}
