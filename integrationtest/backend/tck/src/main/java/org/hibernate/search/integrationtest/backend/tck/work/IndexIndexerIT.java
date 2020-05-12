/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.work;

import static org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils.referenceProvider;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexer;
import org.hibernate.search.engine.backend.work.execution.spi.IndexWorkspace;
import org.hibernate.search.integrationtest.backend.tck.testsupport.configuration.DefaultAnalysisDefinitions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.test.FutureAssert;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.awaitility.Awaitility;

/**
 * Verify that the {@link IndexIndexer}, provided by a backend, is working properly, storing correctly the indexes.
 */
public class IndexIndexerIT {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final int NUMBER_OF_BOOKS = 200;

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	private final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( "MainIndex", IndexBinding::new );

	@Before
	public void setup() {
		setupHelper.start().withIndex( index ).setup();
	}

	@Test
	public void success() {
		IndexIndexer indexer = index.createIndexer();
		CompletableFuture<?>[] tasks = new CompletableFuture<?>[NUMBER_OF_BOOKS];
		IndexWorkspace workspace = index.createWorkspace();

		// Add
		for ( int i = 0; i < NUMBER_OF_BOOKS; i++ ) {
			final String id = String.valueOf( i );
			tasks[i] = indexer.add( referenceProvider( id ), document -> {
				document.addValue( index.binding().title, "The Lord of the Rings chap. " + id );
			} );
		}
		CompletableFuture<?> future = CompletableFuture.allOf( tasks );
		Awaitility.await().until( future::isDone );
		// The operations should succeed.
		FutureAssert.assertThat( future ).isSuccessful();

		int expectedMatchingBooks = NUMBER_OF_BOOKS;
		workspace.refresh().join();
		SearchResultAssert.assertThat( index.createScope().query()
				.where( f -> f.match().field( "title" ).matching( "lord" ) )
				.toQuery() )
				.hasTotalHitCount( expectedMatchingBooks );

		// Update
		int booksToUpdate = NUMBER_OF_BOOKS / 4;
		tasks = new CompletableFuture<?>[booksToUpdate];
		for ( int i = 0; i < booksToUpdate; i++ ) {
			final String id = String.valueOf( i );
			tasks[i] = indexer.update( referenceProvider( id ), document -> {
				document.addValue( index.binding().title, "The Boss of the Rings chap. " + id );
			} );
		}
		future = CompletableFuture.allOf( tasks );
		Awaitility.await().until( future::isDone );
		// The operations should succeed.
		FutureAssert.assertThat( future ).isSuccessful();

		expectedMatchingBooks -= booksToUpdate;
		workspace.refresh().join();
		SearchResultAssert.assertThat( index.createScope().query()
				.where( f -> f.match().field( "title" ).matching( "lord" ) )
				.toQuery() )
				.hasTotalHitCount( expectedMatchingBooks );

		// Delete
		int booksToDelete = NUMBER_OF_BOOKS / 4;
		tasks = new CompletableFuture<?>[booksToDelete];
		for ( int i = 0; i < booksToDelete; i++ ) {
			final String id = String.valueOf( i + booksToUpdate );
			tasks[i] = indexer.delete( referenceProvider( id ) );
		}
		future = CompletableFuture.allOf( tasks );
		Awaitility.await().until( future::isDone );
		// The operations should succeed.
		FutureAssert.assertThat( future ).isSuccessful();

		expectedMatchingBooks -= booksToDelete;
		workspace.refresh().join();
		SearchResultAssert.assertThat( index.createScope().query()
				.where( f -> f.match().field( "title" ).matching( "lord" ) )
				.toQuery() )
				.hasTotalHitCount( expectedMatchingBooks );
	}

	@Test
	public void add_failure() {
		IndexIndexer indexer = index.createIndexer();

		// Trigger failures in the next operations
		setupHelper.getBackendAccessor().ensureIndexOperationsFail( index.name() );

		CompletableFuture<?> future = indexer.add( referenceProvider( "1" ), document -> {
			document.addValue( index.binding().title, "Document #1" );
		} );
		Awaitility.await().until( future::isDone );

		// The operation should fail.
		// Just check the failure is reported through the completable future.
		FutureAssert.assertThat( future ).isFailed();

		try {
			setupHelper.cleanUp();
		}
		catch (RuntimeException | IOException e) {
			log.debug( "Expected error while shutting down Hibernate Search, caused by the deletion of an index", e );
		}
	}

	@Test
	public void update_failure() {
		IndexIndexer indexer = index.createIndexer();

		// Trigger failures in the next operations
		setupHelper.getBackendAccessor().ensureIndexOperationsFail( index.name() );

		CompletableFuture<?> future = indexer.update( referenceProvider( "1" ), document -> {
			document.addValue( index.binding().title, "Document #1" );
		} );
		Awaitility.await().until( future::isDone );

		// The operation should fail.
		// Just check the failure is reported through the completable future.
		FutureAssert.assertThat( future ).isFailed();

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
		setupHelper.getBackendAccessor().ensureIndexOperationsFail( index.name() );

		CompletableFuture<?> future = indexer.delete( referenceProvider( "1" ) );
		Awaitility.await().until( future::isDone );

		// The operation should fail.
		// Just check the failure is reported through the completable future.
		FutureAssert.assertThat( future ).isFailed();

		try {
			setupHelper.cleanUp();
		}
		catch (RuntimeException | IOException e) {
			log.debug( "Expected error while shutting down Hibernate Search, caused by the deletion of an index", e );
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
