package org.hibernate.search.backend.elasticsearch.work.impl;

import org.hibernate.search.backend.elasticsearch.client.common.impl.ElasticsearchJsonRequest;
import org.hibernate.search.backend.elasticsearch.client.common.impl.ElasticsearchJsonResponse;
import org.hibernate.search.backend.elasticsearch.client.common.util.spi.URLEncodedString;

public class DropIndexWork extends AbstractNonBulkableWork<Void> {

	protected DropIndexWork(Builder builder) {
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

		public Builder ignoreIndexNotFound() {
			this.resultAssessor = ElasticsearchRequestSuccessAssessor.builder()
					.ignoreErrorTypes( "index_not_found_exception" )
					.build();

			return this;
		}

		@Override
		protected ElasticsearchJsonRequest buildRequest() {
			ElasticsearchJsonRequest.Builder builder =
					ElasticsearchJsonRequest.delete()
							.pathComponent( indexName );

			return builder.build();
		}

		@Override
		public DropIndexWork build() {
			return new DropIndexWork( this );
		}
	}
}
