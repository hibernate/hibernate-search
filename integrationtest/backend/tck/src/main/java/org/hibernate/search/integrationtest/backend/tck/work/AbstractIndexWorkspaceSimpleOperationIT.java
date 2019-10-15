/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.work;

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
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingIndexManager;
import org.hibernate.search.util.impl.test.FutureAssert;

import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;

import org.awaitility.Awaitility;

public abstract class AbstractIndexWorkspaceSimpleOperationIT {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final String INDEX_NAME = "IndexName";

	private static final Integer DOCUMENT_COUNT = 50;

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	private IndexMapping indexMapping;
	private StubMappingIndexManager indexManager;

	@Test
	public void success() {
		setup();

		IndexWorkspace workspace = indexManager.createWorkspace();

		CompletableFuture<?> future = executeAsync( workspace );
		Awaitility.await().until( future::isDone );

		FutureAssert.assertThat( future ).isSuccessful();

		assertSuccess( indexManager );
	}

	@Test
	public void failure() {
		Assume.assumeTrue( operationWillFailIfAppliedToDeletedIndex() );

		setup();

		IndexWorkspace workspace = indexManager.createWorkspace();

		// Trigger failures in the next operations
		setupHelper.getBackendAccessor().ensureIndexOperationsFail( INDEX_NAME );

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

	protected abstract boolean operationWillFailIfAppliedToDeletedIndex();

	protected abstract CompletableFuture<?> executeAsync(IndexWorkspace workspace);

	protected abstract void assertSuccess(StubMappingIndexManager indexManager);

	private void setup() {
		setupHelper.start()
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
