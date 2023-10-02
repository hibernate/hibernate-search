/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.work;

import static org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils.documentProvider;
import static org.hibernate.search.util.impl.test.FutureAssert.assertThatFuture;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.work.execution.spi.IndexWorkspace;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckBackendAccessor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.common.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.awaitility.Awaitility;

public abstract class AbstractIndexWorkspaceSimpleOperationIT {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final Integer DOCUMENT_COUNT = 50;

	@RegisterExtension
	public final SearchSetupHelper setupHelper;

	private final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	protected AbstractIndexWorkspaceSimpleOperationIT() {
		this( SearchSetupHelper.create() );
	}

	protected AbstractIndexWorkspaceSimpleOperationIT(SearchSetupHelper setupHelper) {
		this.setupHelper = setupHelper;
	}

	@Test
	void success() {
		setup();

		IndexWorkspace workspace = index.createWorkspace();

		assertPreconditions( index );

		CompletableFuture<?> future = executeAsync( workspace );
		Awaitility.await().until( future::isDone );

		assertThatFuture( future ).isSuccessful();

		assertSuccess( index );
	}

	@Test
	void failure() {
		setup();

		IndexWorkspace workspace = index.createWorkspace();

		// Trigger failures in the next operations
		ensureOperationsFail( setupHelper.getBackendAccessor(), index.name() );

		CompletableFuture<?> future = executeAsync( workspace );
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

	protected abstract void ensureOperationsFail(TckBackendAccessor accessor, String indexName);

	protected void beforeInitData(StubMappedIndex index) {
		// Nothing to do by default.
	}

	protected void afterInitData(StubMappedIndex index) {
		// Nothing to do by default.
	}

	protected abstract void assertPreconditions(StubMappedIndex index);

	protected abstract CompletableFuture<?> executeAsync(IndexWorkspace workspace);

	protected abstract void assertSuccess(StubMappedIndex index);

	private void setup() {
		startHelper().withIndex( index ).setup();

		beforeInitData( index );

		index.bulkIndexer( false ) // No refresh
				.add( DOCUMENT_COUNT, i -> documentProvider(
						String.valueOf( i ),
						document -> document.addValue( index.binding().text, "Text #" + i )
				) )
				.join();

		afterInitData( index );
	}

	protected SearchSetupHelper.SetupContext startHelper() {
		return setupHelper.start();
	}

	private static class IndexBinding {
		final IndexFieldReference<String> text;

		IndexBinding(IndexSchemaElement root) {
			text = root.field( "text", f -> f.asString() )
					.toReference();
		}
	}

}
