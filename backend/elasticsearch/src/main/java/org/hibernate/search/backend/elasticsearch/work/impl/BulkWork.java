/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.work.impl;

import java.util.List;

import org.hibernate.search.backend.elasticsearch.client.impl.Paths;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchRequest;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchResponse;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.work.result.impl.BulkResult;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class BulkWork extends AbstractNonBulkableWork<BulkResult> {

	private static final JsonAccessor<JsonArray> BULK_ITEMS = JsonAccessor.root().property( "items" ).asArray();

	protected BulkWork(Builder builder) {
		super( builder );
	}

	@Override
	protected BulkResult generateResult(ElasticsearchWorkExecutionContext context, ElasticsearchResponse response) {
		JsonObject parsedResponseBody = response.body();
		JsonArray resultItems = BULK_ITEMS.get( parsedResponseBody ).orElseGet( JsonArray::new );
		return new BulkResultImpl( resultItems );
	}

	public static class Builder extends AbstractNonBulkableWork.AbstractBuilder<Builder> {
		private final List<? extends BulkableWork<?>> bulkableWorks;

		private DocumentRefreshStrategy refreshStrategy = DocumentRefreshStrategy.NONE;

		public Builder(List<? extends BulkableWork<?>> bulkableWorks) {
			super( ElasticsearchRequestSuccessAssessor.DEFAULT_INSTANCE );
			this.bulkableWorks = bulkableWorks;
		}

		public Builder refresh(DocumentRefreshStrategy refreshStrategy) {
			this.refreshStrategy = refreshStrategy;
			return this;
		}

		@Override
		protected ElasticsearchRequest buildRequest() {
			ElasticsearchRequest.Builder builder =
					ElasticsearchRequest.post()
							.pathComponent( Paths._BULK );
			switch ( refreshStrategy ) {
				case FORCE:
					builder.param( "refresh", true );
					break;
				case NONE:
					break;
			}

			for ( BulkableWork<?> work : bulkableWorks ) {
				builder.body( work.getBulkableActionMetadata() );
				JsonObject actionBody = work.getBulkableActionBody();
				if ( actionBody != null ) {
					builder.body( actionBody );
				}
			}

			return builder.build();
		}

		@Override
		public BulkWork build() {
			return new BulkWork( this );
		}
	}

	private static class BulkResultImpl implements BulkResult {
		private final JsonArray results;

		public BulkResultImpl(JsonArray results) {
			super();
			this.results = results;
		}

		@Override
		public <T> T extract(ElasticsearchWorkExecutionContext context, BulkableWork<T> work, int index) {
			JsonObject bulkItemResponse = results.get( index ).getAsJsonObject();
			return work.handleBulkResult( context, bulkItemResponse );
		}
	}

}
