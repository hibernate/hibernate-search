/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.mapper.stub;

import static org.hibernate.search.util.common.impl.CollectionHelper.asSetIgnoreNull;
import static org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils.documentProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.stream.IntStream;

import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.mapping.spi.BackendMappingContext;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.engine.backend.work.execution.spi.DocumentContributor;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexer;
import org.hibernate.search.engine.backend.work.execution.spi.IndexWorkspace;
import org.hibernate.search.engine.backend.work.execution.spi.UnsupportedOperationBehavior;
import org.hibernate.search.engine.mapper.mapping.spi.MappedIndexManager;
import org.hibernate.search.util.common.impl.Futures;

/**
 * A thread-unsafe util for adding documents to indexes.
 */
public class BulkIndexer {

	private static final int BATCH_SIZE = 250;
	private static final int PARALLELISM = 2;

	private final MappedIndexManager indexManager;
	private final BackendMappingContext mappingContext;
	private final String tenantId;
	private final IndexIndexer indexer;
	private final boolean refreshEachBatch;
	private final boolean refreshAtEnd;

	private List<StubDocumentProvider> buildingBatch = new ArrayList<>();

	private final List<IndexingQueue> indexingQueues;
	private int currentQueueIndex;

	BulkIndexer(MappedIndexManager indexManager, StubSession sessionContext,
			boolean refreshEachBatch, boolean refreshAtEnd) {
		this.indexManager = indexManager;
		this.mappingContext = sessionContext.mappingContext();
		this.tenantId = sessionContext.tenantIdentifier();
		this.indexer = indexManager.createIndexer( sessionContext );
		this.refreshEachBatch = refreshEachBatch;
		this.refreshAtEnd = refreshAtEnd;
		this.indexingQueues = new ArrayList<>( PARALLELISM );
		for ( int i = 0; i < PARALLELISM; i++ ) {
			indexingQueues.add( new IndexingQueue() );
		}
		currentQueueIndex = 0;
	}

	public BulkIndexer add(String identifier, DocumentContributor contributor) {
		return add( documentProvider( identifier, null, contributor ) );
	}

	public BulkIndexer add(String identifier, String routingKey,
			DocumentContributor contributor) {
		return add( documentProvider( identifier, routingKey, contributor ) );
	}

	public BulkIndexer add(StubDocumentProvider documentProvider) {
		buildingBatch.add( documentProvider );
		if ( buildingBatch.size() >= BATCH_SIZE ) {
			sendBatch();
		}
		return this;
	}

	public <T> BulkIndexer add(IndexFieldReference<T> fieldReference,
			Consumer<SingleFieldDocumentBuilder<T>> valueContributor) {
		valueContributor.accept( new SingleFieldDocumentBuilder<T>() {
			@Override
			public void emptyDocument(String documentId) {
				add( documentProvider( documentId, document -> {} ) );
			}

			@Override
			public void document(String documentId, T fieldValue) {
				add( documentProvider( documentId, document -> document.addValue( fieldReference, fieldValue ) ) );
			}
		} );
		return this;
	}

	public BulkIndexer add(int documentCount, IntFunction<StubDocumentProvider> documentProviderGenerator) {
		IntStream.range( 0, documentCount )
				.mapToObj( documentProviderGenerator )
				.forEach( this::add );
		return this;
	}

	public void join() {
		Futures.unwrappedExceptionJoin( end() );
	}

	public void join(BulkIndexer... otherIndexers) {
		CompletableFuture<?>[] futures = new CompletableFuture[otherIndexers.length + 1];
		for ( int i = 0; i < otherIndexers.length; i++ ) {
			futures[i] = otherIndexers[i].end();
		}
		futures[otherIndexers.length] = end();
		Futures.unwrappedExceptionJoin( CompletableFuture.allOf( futures ) );
	}

	private CompletableFuture<?> end() {
		sendBatch();
		CompletableFuture<?>[] indexingFutures = new CompletableFuture[indexingQueues.size()];
		for ( int i = 0; i < indexingQueues.size(); i++ ) {
			indexingFutures[i] = indexingQueues.get( i ).future;
		}
		CompletableFuture<?> future = CompletableFuture.allOf( indexingFutures );
		if ( refreshAtEnd ) {
			IndexWorkspace workspace = indexManager.createWorkspace(
					mappingContext,
					asSetIgnoreNull( tenantId )
			);
			future = future.thenCompose(
					ignored -> workspace.refresh( OperationSubmitter.blocking(), UnsupportedOperationBehavior.FAIL ) );
		}
		return future;
	}

	private void sendBatch() {
		if ( buildingBatch.isEmpty() ) {
			return;
		}
		indexingQueues.get( currentQueueIndex ).sendBatch( buildingBatch );
		buildingBatch = new ArrayList<>();
		currentQueueIndex = ( currentQueueIndex + 1 ) % indexingQueues.size();
	}

	private class IndexingQueue {
		private CompletableFuture<?> future = CompletableFuture.completedFuture( null );

		void sendBatch(List<StubDocumentProvider> batch) {
			int sendingBatchSize = batch.size();
			// Poor man's rate limiting: we only ever send one batch per queue at a time,
			// because Elasticsearch tends to fail if we send too many documents at once.
			future = future.thenCompose( ignored -> {
				CompletableFuture<?>[] batchFutures = new CompletableFuture[sendingBatchSize];
				for ( int i = 0; i < sendingBatchSize; i++ ) {
					StubDocumentProvider documentProvider = batch.get( i );
					batchFutures[i] = indexer.add(
							documentProvider.getReferenceProvider(), documentProvider.getContributor(),
							DocumentCommitStrategy.NONE,
							refreshEachBatch ? DocumentRefreshStrategy.FORCE : DocumentRefreshStrategy.NONE,
							OperationSubmitter.blocking()
					);
				}
				return CompletableFuture.allOf( batchFutures );
			} );
		}
	}
}
