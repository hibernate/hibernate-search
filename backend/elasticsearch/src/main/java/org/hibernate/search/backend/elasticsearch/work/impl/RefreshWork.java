package org.hibernate.search.backend.elasticsearch.work.impl;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.backend.elasticsearch.client.common.impl.ElasticsearchJsonRequest;
import org.hibernate.search.backend.elasticsearch.client.common.impl.ElasticsearchJsonResponse;
import org.hibernate.search.backend.elasticsearch.client.common.util.spi.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.client.impl.Paths;

public class RefreshWork extends AbstractNonBulkableWork<Void> {

	protected RefreshWork(Builder builder) {
		super( builder );
	}

	@Override
	protected Void generateResult(ElasticsearchWorkExecutionContext context, ElasticsearchJsonResponse response) {
		return null;
	}

	public static class Builder
			extends AbstractBuilder<Builder> {
		private final List<URLEncodedString> indexNames = new ArrayList<>();

		public Builder() {
			super( ElasticsearchRequestSuccessAssessor.DEFAULT_INSTANCE );
		}

		public Builder index(URLEncodedString indexName) {
			indexNames.add( indexName );
			return this;
		}

		@Override
		protected ElasticsearchJsonRequest buildRequest() {
			ElasticsearchJsonRequest.Builder builder =
					ElasticsearchJsonRequest.post();

			if ( !indexNames.isEmpty() ) {
				builder.multiValuedPathComponent( indexNames );
			}

			builder.pathComponent( Paths._REFRESH );

			return builder.build();
		}

		@Override
		public RefreshWork build() {
			return new RefreshWork( this );
		}
	}
}
