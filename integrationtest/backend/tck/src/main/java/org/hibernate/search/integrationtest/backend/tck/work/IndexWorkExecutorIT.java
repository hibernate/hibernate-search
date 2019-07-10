/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.work;

import static org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMapperUtils.referenceProvider;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.spi.IndexDocumentWorkExecutor;
import org.hibernate.search.engine.backend.work.execution.spi.IndexWorkExecutor;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckBackendHelper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.common.stub.StubSessionContext;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingIndexManager;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.assertj.core.api.Assertions;

/**
 * Verify that the work executor operations:
 * {@link IndexWorkExecutor#optimize()}, {@link IndexWorkExecutor#purge()}, {@link IndexWorkExecutor#flush()}
 * work properly, in every backends.
 */
public class IndexWorkExecutorIT {

	private static final String TENANT_1 = "tenant1";
	private static final String TENANT_2 = "tenant2";

	private static final String INDEX_NAME = "lordOfTheRingsChapters";
	private static final int NUMBER_OF_BOOKS = 200;

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Rule
	public SearchSetupHelper multiTenancySetupHelper = new SearchSetupHelper( TckBackendHelper::createMultiTenancyBackendSetupStrategy );

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private final StubSessionContext noTenantSessionContext = new StubSessionContext();
	private final StubSessionContext tenant1SessionContext = new StubSessionContext( TENANT_1 );
	private final StubSessionContext tenant2SessionContext = new StubSessionContext( TENANT_2 );

	private IndexMapping indexMapping;
	private StubMappingIndexManager indexManager;

	@Test
	public void runOptimizePurgeAndFlushInSequence() {
		setupHelper.start()
				.withIndex(
						INDEX_NAME,
						ctx -> this.indexMapping = new IndexMapping( ctx.getSchemaElement() ),
						indexManager -> this.indexManager = indexManager
				)
				.setup();

		// Do not provide a tenant
		IndexWorkExecutor workExecutor = indexManager.createWorkExecutor();
		createBookIndexes( noTenantSessionContext );

		workExecutor.flush().join();
		assertBookNumberIsEqualsTo( NUMBER_OF_BOOKS, noTenantSessionContext );

		workExecutor.optimize().join();
		assertBookNumberIsEqualsTo( NUMBER_OF_BOOKS, noTenantSessionContext );

		// purge without providing a tenant
		workExecutor.purge().join();
		workExecutor.flush().join();

		assertBookNumberIsEqualsTo( 0, noTenantSessionContext );
	}

	@Test
	public void runOptimizePurgeAndFlushWithMultiTenancy() {
		multiTenancySetupHelper.start()
				.withIndex(
						INDEX_NAME,
						ctx -> this.indexMapping = new IndexMapping( ctx.getSchemaElement() ),
						indexManager -> this.indexManager = indexManager
				)
				.withMultiTenancy()
				.setup();

		// Do provide a tenant ID
		IndexWorkExecutor workExecutor = indexManager.createWorkExecutor( tenant1SessionContext );

		createBookIndexes( tenant1SessionContext );
		createBookIndexes( tenant2SessionContext );
		workExecutor.flush().join();

		assertBookNumberIsEqualsTo( NUMBER_OF_BOOKS, tenant1SessionContext );
		assertBookNumberIsEqualsTo( NUMBER_OF_BOOKS, tenant2SessionContext );

		workExecutor.optimize().join();
		assertBookNumberIsEqualsTo( NUMBER_OF_BOOKS, tenant1SessionContext );
		assertBookNumberIsEqualsTo( NUMBER_OF_BOOKS, tenant2SessionContext );

		workExecutor.purge().join();
		workExecutor.flush().join();

		// check that only TENANT_1 is affected by the purge
		assertBookNumberIsEqualsTo( 0, tenant1SessionContext );
		assertBookNumberIsEqualsTo( NUMBER_OF_BOOKS, tenant2SessionContext );
	}

	private void createBookIndexes(StubSessionContext sessionContext) {
		IndexDocumentWorkExecutor<? extends DocumentElement> documentWorkExecutor =
				indexManager.createDocumentWorkExecutor( sessionContext, DocumentCommitStrategy.NONE );
		CompletableFuture<?>[] tasks = new CompletableFuture<?>[NUMBER_OF_BOOKS];

		for ( int i = 0; i < NUMBER_OF_BOOKS; i++ ) {
			final String id = i + "";
			tasks[i] = documentWorkExecutor.add( referenceProvider( id ), document -> {
				document.addValue( indexMapping.title, "The Lord of the Rings cap. " + id );
			} );
		}
		CompletableFuture.allOf( tasks ).join();
	}

	private void assertBookNumberIsEqualsTo(long bookNumber, StubSessionContext sessionContext) {
		SearchQuery<DocumentReference> query = indexManager.createScope().query( sessionContext )
				.predicate( f -> f.matchAll() )
				.toQuery();

		Assertions.assertThat( query.fetchTotalHitCount() ).isEqualTo( bookNumber );
	}

	private static class IndexMapping {
		final IndexFieldReference<String> title;

		IndexMapping(IndexSchemaElement root) {
			title = root.field( "title", f -> f.asString() )
					.toReference();
		}
	}
}
