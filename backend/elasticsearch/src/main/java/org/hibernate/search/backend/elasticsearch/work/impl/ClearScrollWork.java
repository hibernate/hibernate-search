package org.hibernate.search.backend.elasticsearch.work.impl;

import org.hibernate.search.backend.elasticsearch.client.common.impl.ElasticsearchJsonRequest;
import org.hibernate.search.backend.elasticsearch.client.common.impl.ElasticsearchJsonResponse;
import org.hibernate.search.backend.elasticsearch.client.impl.Paths;

import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

public class ClearScrollWork extends AbstractNonBulkableWork<Void> {

	protected ClearScrollWork(Builder builder) {
		super( builder );
	}

	@Override
	protected Void generateResult(ElasticsearchWorkExecutionContext context, ElasticsearchJsonResponse response) {
		return null;
	}

	public static class Builder extends AbstractBuilder<Builder> {

		private final String scrollId;

		public Builder(String scrollId) {
			super( ElasticsearchRequestSuccessAssessor.DEFAULT_INSTANCE );
			this.scrollId = scrollId;
		}

		@Override
		protected ElasticsearchJsonRequest buildRequest() {
			ObjectNode body = new ObjectNode( JsonNodeFactory.instance );
			body.put( "scroll_id", scrollId );
			ElasticsearchJsonRequest.Builder builder =
					ElasticsearchJsonRequest.delete().pathComponent( Paths._SEARCH ).pathComponent( Paths.SCROLL )
							.bodyJsonFromInternal( body );
			return builder.build();
		}

		@Override
		public ClearScrollWork build() {
			return new ClearScrollWork( this );
		}
	}
}
