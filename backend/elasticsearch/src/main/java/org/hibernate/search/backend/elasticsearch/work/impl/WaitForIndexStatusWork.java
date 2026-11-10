package org.hibernate.search.backend.elasticsearch.work.impl;

import org.hibernate.search.backend.elasticsearch.client.common.impl.ElasticsearchJsonRequest;
import org.hibernate.search.backend.elasticsearch.client.common.impl.ElasticsearchJsonResponse;
import org.hibernate.search.backend.elasticsearch.client.common.util.spi.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.client.impl.Paths;
import org.hibernate.search.backend.elasticsearch.index.IndexStatus;
import org.hibernate.search.engine.common.timing.spi.StaticDeadline;

public class WaitForIndexStatusWork extends AbstractNonBulkableWork<Void> {

	protected WaitForIndexStatusWork(Builder builder) {
		super( builder );
	}

	@Override
	protected Void generateResult(ElasticsearchWorkExecutionContext context, ElasticsearchJsonResponse response) {
		return null;
	}

	public static class Builder
			extends AbstractBuilder<Builder> {
		private final URLEncodedString indexName;
		private final IndexStatus requiredStatus;
		private final int requiredStatusTimeoutInMs;

		public Builder(URLEncodedString indexName, IndexStatus requiredStatus, int requiredStatusTimeoutInMs) {
			super( ElasticsearchRequestSuccessAssessor.DEFAULT_INSTANCE );
			this.indexName = indexName;
			this.requiredStatus = requiredStatus;
			this.requiredStatusTimeoutInMs = requiredStatusTimeoutInMs;
		}

		@Override
		protected ElasticsearchJsonRequest buildRequest() {
			ElasticsearchJsonRequest.Builder builder =
					ElasticsearchJsonRequest.get()
							.pathComponent( Paths._CLUSTER )
							.pathComponent( Paths.HEALTH )
							.pathComponent( indexName )
							.param( "wait_for_status", requiredStatus.externalRepresentation() )
							.param( "timeout", requiredStatusTimeoutInMs + "ms" );

			builder.deadline( StaticDeadline.ofMilliseconds( requiredStatusTimeoutInMs ) );

			return builder.build();
		}

		@Override
		public WaitForIndexStatusWork build() {
			return new WaitForIndexStatusWork( this );
		}
	}
}
