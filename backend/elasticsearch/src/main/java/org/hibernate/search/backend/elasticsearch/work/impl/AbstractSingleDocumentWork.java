/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.work.impl;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import com.google.gson.JsonObject;


public abstract class AbstractSingleDocumentWork
		implements BulkableWork<Void>, SingleDocumentWork {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final JsonObject bulkableActionMetadata;
	private final JsonObject bulkableActionBody;
	protected final ElasticsearchRequestSuccessAssessor resultAssessor;

	private final String entityTypeName;
	private final Object entityIdentifier;

	private final DocumentRefreshStrategy refreshStrategy;

	protected AbstractSingleDocumentWork(AbstractBuilder<?> builder) {
		this.bulkableActionMetadata = builder.buildBulkableActionMetadata();
		this.bulkableActionBody = builder.buildBulkableActionBody();
		this.resultAssessor = builder.resultAssessor;
		this.entityTypeName = builder.entityTypeName;
		this.entityIdentifier = builder.entityIdentifier;
		this.refreshStrategy = builder.refreshStrategy;
	}

	@Override
	public CompletableFuture<Void> aggregate(ElasticsearchWorkAggregator aggregator) {
		return aggregator.addBulkable( this );
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
		return bulkableActionBody;
	}

	@Override
	public Void handleBulkResult(ElasticsearchWorkExecutionContext context, JsonObject bulkResponseItem) {
		try {
			resultAssessor.checkSuccess( bulkResponseItem );
		}
		catch (RuntimeException e) {
			throw log.elasticsearchBulkedRequestFailed(
					getBulkableActionMetadata(), bulkResponseItem,
					e.getMessage(),
					e
			);
		}

		return null;
	}

	protected abstract static class AbstractBuilder<B> {
		private final ElasticsearchRequestSuccessAssessor resultAssessor;

		private final String entityTypeName;
		private final Object entityIdentifier;

		private DocumentRefreshStrategy refreshStrategy = DocumentRefreshStrategy.NONE;

		public AbstractBuilder(ElasticsearchRequestSuccessAssessor resultAssessor,
				String entityTypeName, Object entityIdentifier) {
			this.resultAssessor = resultAssessor;
			this.entityTypeName = entityTypeName;
			this.entityIdentifier = entityIdentifier;
		}

		public B refresh(DocumentRefreshStrategy refreshStrategy) {
			this.refreshStrategy = refreshStrategy;
			return (B) this;
		}

		protected abstract JsonObject buildBulkableActionMetadata();

		protected abstract JsonObject buildBulkableActionBody();

	}
}
