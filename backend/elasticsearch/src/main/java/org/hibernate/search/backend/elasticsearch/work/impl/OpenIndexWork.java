/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.work.impl;

import org.hibernate.search.backend.elasticsearch.client.impl.Paths;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchRequest;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchResponse;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;

public class OpenIndexWork extends AbstractNonBulkableWork<Void> {

	protected OpenIndexWork(Builder builder) {
		super( builder );
	}

	@Override
	protected Void generateResult(ElasticsearchWorkExecutionContext context, ElasticsearchResponse response) {
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
		protected ElasticsearchRequest buildRequest() {
			ElasticsearchRequest.Builder builder =
					ElasticsearchRequest.post()
							.pathComponent( indexName )
							.pathComponent( Paths._OPEN );

			return builder.build();
		}

		@Override
		public OpenIndexWork build() {
			return new OpenIndexWork( this );
		}
	}
}
