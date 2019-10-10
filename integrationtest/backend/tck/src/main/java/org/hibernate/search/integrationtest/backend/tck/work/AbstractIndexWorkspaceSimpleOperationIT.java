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
import org.hibernate.search.engine.cfg.EngineSettings;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.impl.integrationtest.common.stub.StubFailureHandler;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingIndexManager;
import org.hibernate.search.util.impl.test.FutureAssert;
import org.hibernate.search.util.impl.test.rule.ExpectedLog4jLog;
import org.hibernate.search.util.impl.test.rule.StaticCounters;

import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;

import org.apache.log4j.Level;
import org.awaitility.Awaitility;

public abstract class AbstractIndexWorkspaceSimpleOperationIT {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final String INDEX_NAME = "IndexName";

	private static final Integer DOCUMENT_COUNT = 50;

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

		IndexWorkspace workspace = indexManager.createWorkspace();

		// The default failure handler should not receive any failure
		logged.expectLevel( Level.ERROR ).never();

		CompletableFuture<?> future = executeAsync( workspace );
		Awaitility.await().until( future::isDone );

		FutureAssert.assertThat( future ).isSuccessful();

		assertSuccess( indexManager );
	}

	@Test
	public void failure_defaultHandler() {
		Assume.assumeTrue( operationWillFailIfAppliedToDeletedIndex() );

		setup( null );

		IndexWorkspace workspace = indexManager.createWorkspace();

		// Trigger failures in the next operations
		setupHelper.getBackendAccessor().ensureIndexOperationsFail( INDEX_NAME );

		// The default failure handler should log at the ERROR level
		logged.expectLevel( Level.ERROR ).once();

		CompletableFuture<?> future = executeAsync( workspace );
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
		Assume.assumeTrue( operationWillFailIfAppliedToDeletedIndex() );

		setup( StubFailureHandler.class.getName() );

		IndexWorkspace workspace = indexManager.createWorkspace();

		// Trigger failures in the next operations
		setupHelper.getBackendAccessor().ensureIndexOperationsFail( INDEX_NAME );

		// The default failure handler should not receive any failure
		logged.expectLevel( Level.ERROR ).never();

		assertThat( staticCounters.get( StubFailureHandler.HANDLE_INDEX_CONTEXT ) ).isEqualTo( 0 );
		assertThat( staticCounters.get( StubFailureHandler.HANDLE_GENERIC_CONTEXT ) ).isEqualTo( 0 );

		CompletableFuture<?> future = executeAsync( workspace );
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

	protected abstract boolean operationWillFailIfAppliedToDeletedIndex();

	protected abstract CompletableFuture<?> executeAsync(IndexWorkspace workspace);

	protected abstract void assertSuccess(StubMappingIndexManager indexManager);

	private void setup(String failureHandler) {
		setupHelper.start()
				.withPropertyRadical( EngineSettings.Radicals.BACKGROUND_FAILURE_HANDLER, failureHandler )
				.withIndex(
						INDEX_NAME,
						ctx -> this.indexMapping = new IndexMapping( ctx.getSchemaElement() ),
						indexManager -> this.indexManager = indexManager
				)
				.setup();

		IndexIndexer<? extends DocumentElement> indexer =
				indexManager.createIndexer( DocumentCommitStrategy.NONE );
		CompletableFuture<?>[] tasks = new CompletableFuture<?>[DOCUMENT_COUNT];
		for ( int i = 0; i < DOCUMENT_COUNT; i++ ) {
			final String id = String.valueOf( i );
			tasks[i] = indexer.add( referenceProvider( id ), document -> {
				document.addValue( indexMapping.text, "Text #" + id );
			} );
		}
		CompletableFuture.allOf( tasks ).join();
		indexManager.createWorkspace().flush().join();
	}

	private static class IndexMapping {
		final IndexFieldReference<String> text;

		IndexMapping(IndexSchemaElement root) {
			text = root.field( "text", f -> f.asString() )
					.toReference();
		}
	}

}
