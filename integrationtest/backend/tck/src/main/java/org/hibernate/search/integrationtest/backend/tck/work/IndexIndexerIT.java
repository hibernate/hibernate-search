/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.work;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;
import static org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils.referenceProvider;
import static org.hibernate.search.util.impl.test.FutureAssert.assertThatFuture;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexer;
import org.hibernate.search.integrationtest.backend.tck.testsupport.configuration.DefaultAnalysisDefinitions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.common.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.awaitility.Awaitility;

/**
 * Verify that the {@link IndexIndexer}, provided by a backend, is working properly, storing correctly the indexes.
 */
class IndexIndexerIT {

	public static List<? extends Arguments> params() {
		List<Arguments> params = new ArrayList<>();
		for ( DocumentCommitStrategy commitStrategy : DocumentCommitStrategy.values() ) {
			for ( DocumentRefreshStrategy refreshStrategy : DocumentRefreshStrategy.values() ) {
				params.add( Arguments.of( commitStrategy, refreshStrategy ) );
			}
		}
		return params;
	}

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final int NUMBER_OF_BOOKS = 200;

	@RegisterExtension
	public final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	private final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	@BeforeEach
	void setup() {
		setupHelper.start().withIndex( index ).setup();
	}

	@ParameterizedTest(name = "commit: {0}, refresh: {1}")
	@MethodSource("params")
	void success(DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy) {
		IndexIndexer indexer = index.createIndexer();
		CompletableFuture<?>[] tasks = new CompletableFuture<?>[NUMBER_OF_BOOKS];

		// Add
		for ( int i = 0; i < NUMBER_OF_BOOKS; i++ ) {
			final String id = String.valueOf( i );
			tasks[i] = indexer.add(
					referenceProvider( id ),
					document -> document.addValue( index.binding().title, "The Lord of the Rings chap. " + id ),
					commitStrategy, refreshStrategy, OperationSubmitter.blocking()
			);
		}
		CompletableFuture<?> future = CompletableFuture.allOf( tasks );
		// In the case of commitStrategy=force we're going to do 200 almost concurrent commits,
		// that is very unrealistic use case. So we will use a very large timeout here.
		Awaitility.await().timeout( 30, TimeUnit.SECONDS ).until( future::isDone );
		// The operations should succeed.
		assertThatFuture( future ).isSuccessful();

		int expectedMatchingBooks1 = NUMBER_OF_BOOKS;
		searchAfterIndexChanges( refreshStrategy, () -> assertThatQuery( index.query()
				.where( f -> f.match().field( "title" ).matching( "lord" ) ) )
				.hasTotalHitCount( expectedMatchingBooks1 ) );

		// Update
		int booksToUpdate = NUMBER_OF_BOOKS / 4;
		tasks = new CompletableFuture<?>[booksToUpdate];
		for ( int i = 0; i < booksToUpdate; i++ ) {
			final String id = String.valueOf( i );
			tasks[i] = indexer.addOrUpdate(
					referenceProvider( id ),
					document -> document.addValue( index.binding().title, "The Boss of the Rings chap. " + id ),
					commitStrategy, refreshStrategy, OperationSubmitter.blocking()
			);
		}
		future = CompletableFuture.allOf( tasks );
		// In the case of commitStrategy=force we're going to do 200 almost concurrent commits,
		// that is very unrealistic use case. So we will use a very large timeout here.
		Awaitility.await().timeout( 30, TimeUnit.SECONDS ).until( future::isDone );
		// The operations should succeed.
		assertThatFuture( future ).isSuccessful();

		int expectedMatchingBooks2 = expectedMatchingBooks1 - booksToUpdate;
		searchAfterIndexChanges( refreshStrategy, () -> assertThatQuery( index.query()
				.where( f -> f.match().field( "title" ).matching( "lord" ) ) )
				.hasTotalHitCount( expectedMatchingBooks2 ) );

		// Delete
		int booksToDelete = NUMBER_OF_BOOKS / 4;
		tasks = new CompletableFuture<?>[booksToDelete];
		for ( int i = 0; i < booksToDelete; i++ ) {
			final String id = String.valueOf( i + booksToUpdate );
			tasks[i] = indexer.delete(
					referenceProvider( id ),
					commitStrategy, refreshStrategy, OperationSubmitter.blocking()
			);
		}
		future = CompletableFuture.allOf( tasks );
		// In the case of commitStrategy=force we're going to do 200 almost concurrent commits,
		// that is very unrealistic use case. So we will use a very large timeout here.
		Awaitility.await().timeout( 30, TimeUnit.SECONDS ).until( future::isDone );
		// The operations should succeed.
		assertThatFuture( future ).isSuccessful();

		int expectedMatchingBooks3 = expectedMatchingBooks2 - booksToDelete;
		searchAfterIndexChanges( refreshStrategy, () -> assertThatQuery( index.query()
				.where( f -> f.match().field( "title" ).matching( "lord" ) ) )
				.hasTotalHitCount( expectedMatchingBooks3 ) );
	}

	@ParameterizedTest(name = "commit: {0}, refresh: {1}")
	@MethodSource("params")
	void add_failure(DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy) {
		IndexIndexer indexer = index.createIndexer();

		// Trigger failures in the next operations
		setupHelper.getBackendAccessor().ensureIndexingOperationsFail( index.name() );

		CompletableFuture<?> future = indexer.add(
				referenceProvider( "1" ),
				document -> document.addValue( index.binding().title, "Document #1" ),
				commitStrategy, refreshStrategy, OperationSubmitter.blocking()
		);
		Awaitility.await().until( future::isDone );

		// The operation should fail.
		// Just check the failure is reported through the completable future.
		assertThatFuture( future ).isFailed();

		try {
			setupHelper.cleanUp();
		}
		catch (RuntimeException | IOException e) {
			log.debug( "Expected error while shutting down Hibernate Search, caused by the deletion of an index", e );
		}
	}

	@ParameterizedTest(name = "commit: {0}, refresh: {1}")
	@MethodSource("params")
	void addOrUpdate_failure(DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy) {
		IndexIndexer indexer = index.createIndexer();

		// Trigger failures in the next operations
		setupHelper.getBackendAccessor().ensureIndexingOperationsFail( index.name() );

		CompletableFuture<?> future = indexer.addOrUpdate(
				referenceProvider( "1" ),
				document -> document.addValue( index.binding().title, "Document #1" ),
				commitStrategy, refreshStrategy, OperationSubmitter.blocking()
		);
		Awaitility.await().until( future::isDone );

		// The operation should fail.
		// Just check the failure is reported through the completable future.
		assertThatFuture( future ).isFailed();

		try {
			setupHelper.cleanUp();
		}
		catch (RuntimeException | IOException e) {
			log.debug( "Expected error while shutting down Hibernate Search, caused by the deletion of an index", e );
		}
	}

	@ParameterizedTest(name = "commit: {0}, refresh: {1}")
	@MethodSource("params")
	void delete_failure(DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy) {
		IndexIndexer indexer = index.createIndexer();

		// Trigger failures in the next operations
		setupHelper.getBackendAccessor().ensureIndexingOperationsFail( index.name() );

		CompletableFuture<?> future = indexer.delete(
				referenceProvider( "1" ), commitStrategy, refreshStrategy, OperationSubmitter.blocking()
		);
		Awaitility.await().until( future::isDone );

		// The operation should fail.
		// Just check the failure is reported through the completable future.
		assertThatFuture( future ).isFailed();

		try {
			setupHelper.cleanUp();
		}
		catch (RuntimeException | IOException e) {
			log.debug( "Expected error while shutting down Hibernate Search, caused by the deletion of an index", e );
		}
	}

	private void searchAfterIndexChanges(DocumentRefreshStrategy refreshStrategy, Runnable assertion) {
		if ( DocumentRefreshStrategy.FORCE.equals( refreshStrategy ) ) {
			// Refresh was supposedly already handled
			assertion.run();
		}
		else {
			// Need to handle the refresh
			index.searchAfterIndexChanges( assertion );
		}
	}

	private static class IndexBinding {
		final IndexFieldReference<String> title;

		IndexBinding(IndexSchemaElement root) {
			title = root.field(
					"title",
					f -> f.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name )
			)
					.toReference();
		}
	}
}
