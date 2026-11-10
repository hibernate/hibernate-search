package org.hibernate.search.backend.elasticsearch.work.impl;

import java.util.List;

import org.hibernate.search.backend.elasticsearch.client.common.impl.ElasticsearchJsonRequest;
import org.hibernate.search.backend.elasticsearch.client.common.impl.ElasticsearchJsonResponse;
import org.hibernate.search.backend.elasticsearch.client.impl.Paths;
import org.hibernate.search.backend.elasticsearch.json.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.work.result.impl.BulkResult;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;

import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

public class BulkWork extends AbstractNonBulkableWork<BulkResult> {

	private static final JsonAccessor<ArrayNode> BULK_ITEMS = JsonAccessor.root().property( "items" ).asArray();

	protected BulkWork(Builder builder) {
		super( builder );
	}

	@Override
	protected BulkResult generateResult(ElasticsearchWorkExecutionContext context, ElasticsearchJsonResponse response) {
		ObjectNode parsedResponseBody = response.bodyJson();
		ArrayNode resultItems =
				BULK_ITEMS.get( parsedResponseBody ).orElseGet( () -> new ArrayNode( JsonNodeFactory.instance ) );
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
		protected ElasticsearchJsonRequest buildRequest() {
			ElasticsearchJsonRequest.Builder builder = ElasticsearchJsonRequest.post().pathComponent( Paths._BULK );
			switch ( refreshStrategy ) {
				case FORCE:
					builder.param( "refresh", true );
					break;
				case NONE:
					break;
			}
			for ( BulkableWork<?> work : bulkableWorks ) {
				builder.bodyJsonFromInternal( work.getBulkableActionMetadata() );
				ObjectNode actionBody = work.getBulkableActionBody();
				if ( actionBody != null ) {
					builder.bodyJsonFromInternal( actionBody );
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

		private final ArrayNode results;

		public BulkResultImpl(ArrayNode results) {
			super();
			this.results = results;
		}

		@Override
		public <T> T extract(ElasticsearchWorkExecutionContext context, BulkableWork<T> work, int index) {
			ObjectNode bulkItemResponse = ( (ObjectNode) results.get( index ) );
			return work.handleBulkResult( context, bulkItemResponse );
		}
	}
}
