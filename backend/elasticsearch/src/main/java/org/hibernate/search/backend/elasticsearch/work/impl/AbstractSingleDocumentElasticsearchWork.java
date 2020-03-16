/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.work.impl;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchRequest;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import com.google.gson.JsonObject;


public abstract class AbstractSingleDocumentElasticsearchWork<R>
		extends AbstractSimpleElasticsearchWork<R>
		implements BulkableElasticsearchWork<R>, SingleDocumentElasticsearchWork<R> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final JsonObject bulkableActionMetadata;

	private final String entityTypeName;
	private final Object entityIdentifier;

	protected AbstractSingleDocumentElasticsearchWork(AbstractBuilder<?> builder) {
		super( builder );
		this.bulkableActionMetadata = builder.buildBulkableActionMetadata();
		this.entityTypeName = builder.entityTypeName;
		this.entityIdentifier = builder.entityIdentifier;
	}

	@Override
	public String getEntityTypeName() {
		return entityTypeName;
	}

	@Override
	public Object getEntityIdentifier() {
		return entityIdentifier;
	}

	@Override
	public DocumentRefreshStrategy getRefreshStrategy() {
		return refreshStrategy;
	}

	@Override
	public JsonObject getBulkableActionMetadata() {
		return bulkableActionMetadata;
	}

	@Override
	public JsonObject getBulkableActionBody() {
		List<JsonObject> bodyParts = request.getBodyParts();
		if ( !bodyParts.isEmpty() ) {
			if ( bodyParts.size() > 1 ) {
				throw new AssertionFailure( "Found a bulkable action with multiple body parts: " + bodyParts );
			}
			return bodyParts.get( 0 );
		}
		else {
			return null;
		}
	}

	@Override
	public R handleBulkResult(ElasticsearchWorkExecutionContext context, JsonObject bulkResponseItem) {
		return handleResult( context, bulkResponseItem );
	}

	@Override
	protected final CompletableFuture<?> beforeExecute(ElasticsearchWorkExecutionContext executionContext, ElasticsearchRequest request) {
		/*
		 * Making this method final so that it won't be overridden:
		 * this method is not used when the work is bulked
		 */
		return super.beforeExecute( executionContext, request );
	}

	@Override
	public CompletableFuture<R> aggregate(ElasticsearchWorkAggregator aggregator) {
		return aggregator.addBulkable( this );
	}

	protected abstract R generateResult(ElasticsearchWorkExecutionContext context, JsonObject bulkResponseItem);

	private R handleResult(ElasticsearchWorkExecutionContext executionContext, JsonObject bulkResponseItem) {
		R result;
		try {
			resultAssessor.checkSuccess( bulkResponseItem );

			result = generateResult( executionContext, bulkResponseItem );

			switch ( refreshStrategy ) {
				case FORCE:
					executionContext.registerIndexToRefresh( refreshedIndexName );
					break;
				case NONE:
					break;
			}
		}
		catch (RuntimeException e) {
			throw log.elasticsearchBulkedRequestFailed(
					getBulkableActionMetadata(), bulkResponseItem,
					e.getMessage(),
					e
			);
		}

		return result;
	}

	protected abstract static class AbstractBuilder<B>
			extends AbstractSimpleElasticsearchWork.AbstractBuilder<B> {

		private final String entityTypeName;
		private final Object entityIdentifier;

		public AbstractBuilder(URLEncodedString dirtiedIndexName, ElasticsearchRequestSuccessAssessor resultAssessor,
				String entityTypeName, Object entityIdentifier) {
			super( dirtiedIndexName, resultAssessor );
			this.entityTypeName = entityTypeName;
			this.entityIdentifier = entityIdentifier;
		}

		protected abstract JsonObject buildBulkableActionMetadata();

	}
}
