/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.work;

import static org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils.documentProvider;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.work.execution.spi.IndexWorkspace;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckBackendAccessor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingIndexManager;
import org.hibernate.search.util.impl.test.FutureAssert;

import org.junit.Rule;
import org.junit.Test;

import org.awaitility.Awaitility;

public abstract class AbstractIndexWorkspaceSimpleOperationIT {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final Integer DOCUMENT_COUNT = 50;

	@Rule
	public final SearchSetupHelper setupHelper;

	private final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	protected AbstractIndexWorkspaceSimpleOperationIT() {
		this( new SearchSetupHelper() );
	}

	protected AbstractIndexWorkspaceSimpleOperationIT(SearchSetupHelper setupHelper) {
		this.setupHelper = setupHelper;
	}

	@Test
	public void success() {
		setup();

		IndexWorkspace workspace = index.createWorkspace();

		assertPreconditions( index );

		CompletableFuture<?> future = executeAsync( workspace );
		Awaitility.await().until( future::isDone );

		FutureAssert.assertThat( future ).isSuccessful();

		assertSuccess( index );
	}

	@Test
	public void failure() {
		setup();

		IndexWorkspace workspace = index.createWorkspace();

		// Trigger failures in the next operations
		ensureOperationsFail( setupHelper.getBackendAccessor(), index.name() );

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

	protected abstract void ensureOperationsFail(TckBackendAccessor accessor, String indexName);

	protected void beforeInitData(StubMappingIndexManager index) {
		// Nothing to do by default.
	}

	protected void afterInitData(StubMappingIndexManager index) {
		// Nothing to do by default.
	}

	protected abstract void assertPreconditions(StubMappingIndexManager index);

	protected abstract CompletableFuture<?> executeAsync(IndexWorkspace workspace);

	protected abstract void assertSuccess(StubMappingIndexManager index);

	private void setup() {
		setupHelper.start().withIndex( index ).setup();

		beforeInitData( index );

		index.initAsync(
				DOCUMENT_COUNT, i -> documentProvider(
						String.valueOf( i ),
						document -> document.addValue( index.binding().text, "Text #" + i )
				),
				false // No refresh
		).join();

		afterInitData( index );
	}

	private static class IndexBinding {
		final IndexFieldReference<String> text;

		IndexBinding(IndexSchemaElement root) {
			text = root.field( "text", f -> f.asString() )
					.toReference();
		}
	}

}
