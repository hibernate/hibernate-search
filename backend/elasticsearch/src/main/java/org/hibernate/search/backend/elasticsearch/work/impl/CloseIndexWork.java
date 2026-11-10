package org.hibernate.search.backend.elasticsearch.work.impl;

import org.hibernate.search.backend.elasticsearch.client.common.impl.ElasticsearchJsonRequest;
import org.hibernate.search.backend.elasticsearch.client.common.impl.ElasticsearchJsonResponse;
import org.hibernate.search.backend.elasticsearch.client.common.util.spi.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.client.impl.Paths;

public class CloseIndexWork extends AbstractNonBulkableWork<Void> {

	protected CloseIndexWork(Builder builder) {
		super( builder );
	}

	@Override
	protected Void generateResult(ElasticsearchWorkExecutionContext context, ElasticsearchJsonResponse response) {
		return null;
	}

	public static class Builder
			extends AbstractBuilder<Builder> {

		private final URLEncodedString indexName;

		public Builder(URLEncodedString indexName) {
			super( ElasticsearchRequestSuccessAssessor.DEFAULT_INSTANCE );
			this.indexName = indexName;
		}

		@Override
		protected ElasticsearchJsonRequest buildRequest() {
			ElasticsearchJsonRequest.Builder builder =
					ElasticsearchJsonRequest.post()
							.pathComponent( indexName )
							.pathComponent( Paths._CLOSE );

			return builder.build();
		}

		@Override
		public CloseIndexWork build() {
			return new CloseIndexWork( this );
		}
	}
}
