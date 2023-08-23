/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
import org.hibernate.search.engine.backend.work.execution.spi.IndexWorkspace;
import org.hibernate.search.engine.backend.work.execution.spi.UnsupportedOperationBehavior;
import org.hibernate.search.integrationtest.backend.tck.testsupport.configuration.DefaultAnalysisDefinitions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import org.awaitility.Awaitility;

/**
 * Verify that the {@link IndexIndexer}, provided by a backend, is working properly, storing correctly the indexes.
 */
@RunWith(Parameterized.class)
public class IndexIndexerIT {

	@Parameterized.Parameters(name = "commit: {0}, refresh: {1}")
	public static List<Object[]> parameters() {
		List<Object[]> params = new ArrayList<>();
		for ( DocumentCommitStrategy commitStrategy : DocumentCommitStrategy.values() ) {
			for ( DocumentRefreshStrategy refreshStrategy : DocumentRefreshStrategy.values() ) {
				params.add( new Object[] { commitStrategy, refreshStrategy } );
			}
		}
		return params;
	}

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final int NUMBER_OF_BOOKS = 200;

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	private final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	private final DocumentCommitStrategy commitStrategy;
	private final DocumentRefreshStrategy refreshStrategy;

	public IndexIndexerIT(DocumentCommitStrategy commitStrategy,
			DocumentRefreshStrategy refreshStrategy) {
		this.commitStrategy = commitStrategy;
		this.refreshStrategy = refreshStrategy;
	}

	@Before
	public void setup() {
		setupHelper.start().withIndex( index ).setup();
	}

	@Test
	public void success() {
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

		int expectedMatchingBooks = NUMBER_OF_BOOKS;
		refreshIfNecessary();
		assertThatQuery( index.createScope().query()
				.where( f -> f.match().field( "title" ).matching( "lord" ) )
				.toQuery() )
				.hasTotalHitCount( expectedMatchingBooks );

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

		expectedMatchingBooks -= booksToUpdate;
		refreshIfNecessary();
		assertThatQuery( index.createScope().query()
				.where( f -> f.match().field( "title" ).matching( "lord" ) )
				.toQuery() )
				.hasTotalHitCount( expectedMatchingBooks );

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

		expectedMatchingBooks -= booksToDelete;
		refreshIfNecessary();
		assertThatQuery( index.createScope().query()
				.where( f -> f.match().field( "title" ).matching( "lord" ) )
				.toQuery() )
				.hasTotalHitCount( expectedMatchingBooks );
	}

	@Test
	public void add_failure() {
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

	@Test
	public void addOrUpdate_failure() {
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

	@Test
	public void delete_failure() {
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

	private void refreshIfNecessary() {
		if ( DocumentRefreshStrategy.NONE.equals( refreshStrategy ) ) {
			IndexWorkspace workspace = index.createWorkspace();
			workspace.refresh( OperationSubmitter.blocking(), UnsupportedOperationBehavior.FAIL ).join();
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
