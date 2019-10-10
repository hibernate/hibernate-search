/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.work;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMapperUtils.referenceProvider;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexer;
import org.hibernate.search.engine.backend.work.execution.spi.IndexWorkspace;
import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.cfg.EngineSettings;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.impl.integrationtest.common.stub.StubFailureHandler;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingIndexManager;
import org.hibernate.search.util.impl.test.FutureAssert;
import org.hibernate.search.util.impl.test.rule.ExpectedLog4jLog;
import org.hibernate.search.util.impl.test.rule.StaticCounters;

import org.junit.Rule;
import org.junit.Test;

import org.apache.log4j.Level;
import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;

/**
 * Verify that the {@link IndexIndexer}, provided by a backend, is working properly, storing correctly the indexes.
 */
public class IndexIndexerIT {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final String INDEX_NAME = "lordOfTheRingsChapters";
	private static final int NUMBER_OF_BOOKS = 200;

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Rule
	public ExpectedLog4jLog logged = ExpectedLog4jLog.create();

	@Rule
	public StaticCounters staticCounters = new StaticCounters();

	private IndexMapping indexMapping;
	private StubMappingIndexManager indexManager;

	@Test
	public void success() {
		setup( null );

		IndexIndexer<? extends DocumentElement> indexer =
				indexManager.createIndexer( DocumentCommitStrategy.NONE );
		CompletableFuture<?>[] tasks = new CompletableFuture<?>[NUMBER_OF_BOOKS];
		IndexWorkspace workspace = indexManager.createWorkspace();

		for ( int i = 0; i < NUMBER_OF_BOOKS; i++ ) {
			final String id = i + "";
			tasks[i] = indexer.add( referenceProvider( id ), document -> {
				document.addValue( indexMapping.title, "The Lord of the Rings cap. " + id );
			} );
		}
		CompletableFuture<?> future = CompletableFuture.allOf( tasks );
		Awaitility.await().until( future::isDone );

		// The operations should succeed.
		FutureAssert.assertThat( future ).isSuccessful();

		workspace.flush().join();

		SearchQuery<DocumentReference> query = indexManager.createScope().query()
				.predicate( f -> f.matchAll() )
				.toQuery();

		Assertions.assertThat( query.fetchTotalHitCount() ).isEqualTo( NUMBER_OF_BOOKS );
	}

	@Test
	public void failure_defaultHandler() {
		setup( null );

		IndexIndexer<? extends DocumentElement> indexer =
				indexManager.createIndexer( DocumentCommitStrategy.NONE );

		// Trigger failures in the next operations
		setupHelper.getBackendAccessor().ensureIndexOperationsFail( INDEX_NAME );

		// The default failure handler should log at the ERROR level
		logged.expectLevel( Level.ERROR ).once();

		CompletableFuture<?> future = indexer.add( referenceProvider( "1" ), document -> {
			document.addValue( indexMapping.title, "Document #1" );
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
	public void failure_customHandler() {
		setup( StubFailureHandler.class.getName() );

		IndexIndexer<? extends DocumentElement> indexer =
				indexManager.createIndexer( DocumentCommitStrategy.NONE );

		// Trigger failures in the next operations
		setupHelper.getBackendAccessor().ensureIndexOperationsFail( INDEX_NAME );

		// The default failure handler should not receive any failure
		logged.expectLevel( Level.ERROR ).never();

		assertThat( staticCounters.get( StubFailureHandler.HANDLE_INDEX_CONTEXT ) ).isEqualTo( 0 );
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_GENERIC_CONTEXT ) ).isEqualTo( 0 );

		CompletableFuture<?> future = indexer.add( referenceProvider( "1" ), document -> {
			document.addValue( indexMapping.title, "Document #1" );
		} );
		Awaitility.await().until( future::isDone );

		// The operation should fail.
		// Just check the failure is reported through the completable future.
		FutureAssert.assertThat( future ).isFailed();

		// The custom failure handler should have received a failure
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_INDEX_CONTEXT ) ).isEqualTo( 1 );
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_GENERIC_CONTEXT ) ).isEqualTo( 0 );

		try {
			setupHelper.cleanUp();
		}
		catch (RuntimeException | IOException e) {
			log.debug( "Expected error while shutting down Hibernate Search, caused by the deletion of an index", e );
		}
	}

	private void setup(String failureHandler) {
		setupHelper.start()
				.withPropertyRadical( EngineSettings.Radicals.BACKGROUND_FAILURE_HANDLER, failureHandler )
				.withIndex(
						INDEX_NAME,
						ctx -> this.indexMapping = new IndexMapping( ctx.getSchemaElement() ),
						indexManager -> this.indexManager = indexManager
				)
				.setup();
	}

	private static class IndexMapping {
		final IndexFieldReference<String> title;

		IndexMapping(IndexSchemaElement root) {
			title = root.field( "title", f -> f.asString() ).toReference();
		}
	}
}
