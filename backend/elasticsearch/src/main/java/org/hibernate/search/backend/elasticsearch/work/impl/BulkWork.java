/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.work.impl;

import java.util.List;

import org.hibernate.search.backend.elasticsearch.client.impl.Paths;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchRequest;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchResponse;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.work.builder.impl.BulkWorkBuilder;
import org.hibernate.search.backend.elasticsearch.work.result.impl.BulkResult;
import org.hibernate.search.backend.elasticsearch.work.result.impl.BulkResultItemExtractor;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;


public class BulkWork extends AbstractNonBulkableElasticsearchWork<BulkResult> {

	private static final JsonAccessor<JsonArray> BULK_ITEMS = JsonAccessor.root().property( "items" ).asArray();

	protected BulkWork(Builder builder) {
		super( builder );
	}

	@Override
	protected BulkResult generateResult(ElasticsearchWorkExecutionContext context, ElasticsearchResponse response) {
		JsonObject parsedResponseBody = response.getBody();
		JsonArray resultItems = BULK_ITEMS.get( parsedResponseBody ).orElseGet( JsonArray::new );
		return new BulkResultImpl( resultItems );
	}

	public static class Builder extends AbstractNonBulkableElasticsearchWork.AbstractBuilder<Builder>
			implements BulkWorkBuilder {
		private final List<? extends BulkableElasticsearchWork<?>> bulkableWorks;

		private DocumentRefreshStrategy refreshStrategy = DocumentRefreshStrategy.NONE;

		public Builder(List<? extends BulkableElasticsearchWork<?>> bulkableWorks) {
			super( DefaultElasticsearchRequestSuccessAssessor.INSTANCE );
			this.bulkableWorks = bulkableWorks;
		}

		@Override
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

			for ( BulkableElasticsearchWork<?> work : bulkableWorks ) {
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
			this.results = results;
		}

		@Override
		public BulkResultItemExtractor withContext(ElasticsearchWorkExecutionContext context) {
			return new BulkResultItemExtractorImpl( results, context );
		}
	}

	private static class BulkResultItemExtractorImpl implements BulkResultItemExtractor {
		private final JsonArray results;

		private final ElasticsearchWorkExecutionContext context;


		public BulkResultItemExtractorImpl(JsonArray results, ElasticsearchWorkExecutionContext context) {
			super();
			this.results = results;
			this.context = context;
		}

		@Override
		public <T> T extract(BulkableElasticsearchWork<T> work, int index) {
			JsonObject bulkItemResponse = results.get( index ).getAsJsonObject();
			return work.handleBulkResult( context, bulkItemResponse );
		}

	}

}
