/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.mapper.stub;

import static org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils.documentProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.IntFunction;
import java.util.stream.IntStream;

import org.hibernate.search.engine.backend.session.spi.DetachedBackendSessionContext;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.backend.work.execution.spi.DocumentContributor;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexer;
import org.hibernate.search.engine.backend.work.execution.spi.IndexWorkspace;
import org.hibernate.search.engine.mapper.mapping.spi.MappedIndexManager;
import org.hibernate.search.util.common.impl.Futures;

/**
 * A thread-unsafe util for adding documents to indexes.
 */
public class BulkIndexer {

	private static final int BATCH_SIZE = 500;

	private final MappedIndexManager indexManager;
	private final DetachedBackendSessionContext sessionContext;
	private final IndexIndexer indexer;
	private final boolean refresh;

	private List<StubDocumentProvider> buildingBatch = new ArrayList<>();
	private CompletableFuture<?> future = CompletableFuture.completedFuture( null );

	BulkIndexer(MappedIndexManager indexManager, StubBackendSessionContext sessionContext, boolean refresh) {
		this.indexManager = indexManager;
		this.sessionContext = DetachedBackendSessionContext.of( sessionContext );
		this.indexer = indexManager.createIndexer( sessionContext );
		this.refresh = refresh;
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

	public BulkIndexer add(StubDocumentProvider ... documentProviders) {
		for ( StubDocumentProvider documentProvider : documentProviders ) {
			add( documentProvider );
		}
		return this;
	}

	public BulkIndexer add(Iterable<? extends StubDocumentProvider> documentProviders) {
		for ( StubDocumentProvider documentProvider : documentProviders ) {
			add( documentProvider );
		}
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

	public void join(BulkIndexer ... otherIndexers) {
		CompletableFuture<?>[] futures = new CompletableFuture[otherIndexers.length + 1];
		for ( int i = 0; i < otherIndexers.length; i++ ) {
			futures[i] = otherIndexers[i].end();
		}
		futures[otherIndexers.length] = end();
		Futures.unwrappedExceptionJoin( CompletableFuture.allOf( futures ) );
	}

	private CompletableFuture<?> end() {
		sendBatch();
		if ( refresh ) {
			IndexWorkspace workspace = indexManager.createWorkspace( sessionContext );
			future = future.thenCompose( ignored -> workspace.refresh() );
		}
		return future;
	}

	private void sendBatch() {
		if ( buildingBatch.isEmpty() ) {
			return;
		}
		List<StubDocumentProvider> sendingBatch = buildingBatch;
		buildingBatch = new ArrayList<>();
		int sendingBatchSize = sendingBatch.size();
		// Poor man's rate limiting: we only ever send one batch at a time,
		// because Elasticsearch tends to fail if we send too many documents at once.
		future = future.thenCompose( ignored -> {
			CompletableFuture<?>[] batchFutures = new CompletableFuture[sendingBatchSize];
			for ( int i = 0; i < sendingBatchSize; i++ ) {
				StubDocumentProvider documentProvider = sendingBatch.get( i );
				batchFutures[i] = indexer.add(
						documentProvider.getReferenceProvider(), documentProvider.getContributor(),
						DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE
				);
			}
			return CompletableFuture.allOf( batchFutures );
		} );
	}

}
