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
import org.hibernate.search.engine.backend.document.IndexFieldAccessor;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.index.spi.IndexDocumentWorkExecutor;
import org.hibernate.search.engine.backend.index.spi.IndexWorkExecutor;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.common.stub.StubSessionContext;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingIndexManager;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.assertj.core.api.Assertions;

/**
 * Verify that the work executor operations:
 * {@link IndexWorkExecutor#optimize()}, {@link IndexWorkExecutor#purge(String)}, {@link IndexWorkExecutor#flush()}
 * work properly, in every backends.
 */
public class IndexWorkExecutorIT {

	private static final String CONFIGURATION_ID = "multi-tenancy";

	private static final String TENANT_1 = "tenant1";
	private static final String TENANT_2 = "tenant2";

	private static final String INDEX_NAME = "lordOfTheRingsChapters";
	private static final int NUMBER_OF_BOOKS = 200;

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private IndexAccessors indexAccessors;
	private StubMappingIndexManager indexManager;

	private StubSessionContext noTenantSessionContext = new StubSessionContext();
	private StubSessionContext tenant1SessionContext = new StubSessionContext( TENANT_1 );
	private StubSessionContext tenant2SessionContext = new StubSessionContext( TENANT_2 );

	@Test
	public void runOptimizePurgeAndFlushInSequence() {
		setupHelper.withDefaultConfiguration()
				.withIndex(
						"MappedType", INDEX_NAME,
						ctx -> this.indexAccessors = new IndexAccessors( ctx.getSchemaElement() ),
						indexManager -> this.indexManager = indexManager
				)
				.setup();

		IndexWorkExecutor workExecutor = indexManager.createWorkExecutor();
		createBookIndexes( noTenantSessionContext );

		workExecutor.flush().join();
		assertBookNumberIsEqualsTo( NUMBER_OF_BOOKS, noTenantSessionContext );

		workExecutor.optimize().join();
		assertBookNumberIsEqualsTo( NUMBER_OF_BOOKS, noTenantSessionContext );

		// purge without providing a tenant
		workExecutor.purge( null ).join();
		workExecutor.flush().join();

		assertBookNumberIsEqualsTo( 0, noTenantSessionContext );
	}

	@Test
	public void runOptimizePurgeAndFlushWithMultiTenancy() {
		setupHelper.withConfiguration( CONFIGURATION_ID )
				.withIndex(
						"MappedType", INDEX_NAME,
						ctx -> this.indexAccessors = new IndexAccessors( ctx.getSchemaElement() ),
						indexManager -> this.indexManager = indexManager
				)
				.withMultiTenancy()
				.setup();

		IndexWorkExecutor workExecutor = indexManager.createWorkExecutor();

		createBookIndexes( tenant1SessionContext );
		createBookIndexes( tenant2SessionContext );
		workExecutor.flush().join();

		assertBookNumberIsEqualsTo( NUMBER_OF_BOOKS, tenant1SessionContext );
		assertBookNumberIsEqualsTo( NUMBER_OF_BOOKS, tenant2SessionContext );

		workExecutor.optimize().join();
		assertBookNumberIsEqualsTo( NUMBER_OF_BOOKS, tenant1SessionContext );
		assertBookNumberIsEqualsTo( NUMBER_OF_BOOKS, tenant2SessionContext );

		// purge only TENANT_1
		workExecutor.purge( TENANT_1 ).join();
		workExecutor.flush().join();

		// so that it will affect only TENANT_1
		assertBookNumberIsEqualsTo( 0, tenant1SessionContext );
		assertBookNumberIsEqualsTo( NUMBER_OF_BOOKS, tenant2SessionContext );
	}

	private void createBookIndexes(StubSessionContext sessionContext) {
		IndexDocumentWorkExecutor<? extends DocumentElement> documentWorkExecutor = indexManager.createDocumentWorkExecutor( sessionContext );
		CompletableFuture[] tasks = new CompletableFuture[NUMBER_OF_BOOKS];

		for ( int i = 0; i < NUMBER_OF_BOOKS; i++ ) {
			final String id = i + "";
			tasks[i] = documentWorkExecutor.add( referenceProvider( id ), document -> {
				indexAccessors.title.write( document, "The Lord of the Rings cap. " + id );
			} );
		}
		CompletableFuture.allOf( tasks ).join();
	}

	private void assertBookNumberIsEqualsTo(long bookNumber, StubSessionContext sessionContext) {
		SearchQuery<DocumentReference> query = indexManager.createSearchTarget().query( sessionContext )
				.asReference()
				.predicate( f -> f.matchAll() )
				.build();

		Assertions.assertThat( query.executeCount() ).isEqualTo( bookNumber );
	}

	private static class IndexAccessors {
		final IndexFieldAccessor<String> title;

		IndexAccessors(IndexSchemaElement root) {
			title = root.field( "title", f -> f.asString().toIndexFieldType() )
					.createAccessor();
		}
	}
}
