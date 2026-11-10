package org.hibernate.search.backend.elasticsearch.work.impl;

import java.util.Map;

import org.hibernate.search.backend.elasticsearch.client.common.impl.ElasticsearchJsonRequest;
import org.hibernate.search.backend.elasticsearch.client.common.impl.ElasticsearchJsonResponse;
import org.hibernate.search.backend.elasticsearch.client.common.json.internal.spi.JsonProvider;
import org.hibernate.search.backend.elasticsearch.client.common.util.spi.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.client.impl.Paths;
import org.hibernate.search.backend.elasticsearch.json.internal.spi.JsonCodecService;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.aliases.impl.IndexAliasDefinition;

import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

public class PutIndexAliasesWork extends AbstractNonBulkableWork<Void> {

	protected PutIndexAliasesWork(Builder builder) {
		super( builder );
	}

	@Override
	protected Void generateResult(ElasticsearchWorkExecutionContext context, ElasticsearchJsonResponse response) {
		return null;
	}

	public static class Builder extends AbstractBuilder<Builder> {

		private final ObjectNode payload;

		public Builder(JsonProvider jsonProvider, URLEncodedString indexName, Map<String, IndexAliasDefinition> aliases) {
			super( ElasticsearchRequestSuccessAssessor.DEFAULT_INSTANCE );
			this.payload = createPayload( jsonProvider, indexName.original, aliases );
		}

		@Override
		protected ElasticsearchJsonRequest buildRequest() {
			ElasticsearchJsonRequest.Builder builder =
					ElasticsearchJsonRequest.post().pathComponent( Paths._ALIASES ).bodyJsonFromInternal( payload );
			return builder.build();
		}

		@Override
		public PutIndexAliasesWork build() {
			return new PutIndexAliasesWork( this );
		}

		private static ObjectNode createPayload(JsonProvider jsonProvider, String indexName,
				Map<String, IndexAliasDefinition> aliases) {
			/*
			 * Serializing nulls is really not a good idea here, it triggers NPEs in Elasticsearch
			 * We better not include the null fields.
			 */
			JsonCodecService json = jsonProvider.getCodecNoSerializeNulls();
			ObjectNode payload = new ObjectNode( JsonNodeFactory.instance );
			ArrayNode actions = new ArrayNode( JsonNodeFactory.instance );
			payload.set( "actions", actions );
			for ( Map.Entry<String, IndexAliasDefinition> entry : aliases.entrySet() ) {
				ObjectNode action = new ObjectNode( JsonNodeFactory.instance );
				ObjectNode aliasDefinition = ( (ObjectNode) json.encode( entry.getValue() ) );
				action.set( "add", aliasDefinition );
				aliasDefinition.put( "index", indexName );
				aliasDefinition.put( "alias", entry.getKey() );
				actions.add( action );
			}
			return payload;
		}
	}
}
