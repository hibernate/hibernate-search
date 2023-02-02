/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.work;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils.documentProvider;

import java.util.Collections;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.work.execution.spi.IndexWorkspace;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckBackendHelper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubSession;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Verify that the work executor operations:
 * {@link IndexWorkspace#mergeSegments(OperationSubmitter)}, {@link IndexWorkspace#purge(java.util.Set, OperationSubmitter)}, {@link IndexWorkspace#flush(OperationSubmitter)}, {@link IndexWorkspace#refresh(OperationSubmitter)}
 * work properly, in every backends.
 */
public class IndexWorkspaceIT {

	private static final String TENANT_1 = "tenant1";
	private static final String TENANT_2 = "tenant2";

	private static final int NUMBER_OF_BOOKS = 200;

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Rule
	public SearchSetupHelper multiTenancySetupHelper = new SearchSetupHelper( TckBackendHelper::createMultiTenancyBackendSetupStrategy );

	private final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	@Before
	public void initSessionContexts() {
	}

	@Test
	public void runMergeSegmentsPurgeAndFlushAndRefreshInSequence() {
		setupHelper.start().withIndex( index ).setup();
		StubSession noTenantSessionContext = index.mapping().session();

		// Do not provide a tenant
		IndexWorkspace workspace = index.createWorkspace();
		createBookIndexes( noTenantSessionContext );

		workspace.refresh( OperationSubmitter.blocking() ).join();
		assertBookNumberIsEqualsTo( NUMBER_OF_BOOKS, noTenantSessionContext );

		workspace.mergeSegments( OperationSubmitter.blocking() ).join();
		assertBookNumberIsEqualsTo( NUMBER_OF_BOOKS, noTenantSessionContext );

		// purge without providing a tenant
		workspace.purge( Collections.emptySet(), OperationSubmitter.blocking() ).join();
		workspace.flush( OperationSubmitter.blocking() ).join();
		workspace.refresh( OperationSubmitter.blocking() ).join();

		assertBookNumberIsEqualsTo( 0, noTenantSessionContext );
	}

	@Test
	public void runMergeSegmentsPurgeAndFlushAndRefreshWithMultiTenancy() {
		multiTenancySetupHelper.start().withIndex( index ).withMultiTenancy().setup();
		StubSession tenant1SessionContext = index.mapping().session( TENANT_1 );
		StubSession tenant2SessionContext = index.mapping().session( TENANT_2 );

		// Do provide a tenant ID
		IndexWorkspace workspace = index.createWorkspace( tenant1SessionContext );

		createBookIndexes( tenant1SessionContext );
		createBookIndexes( tenant2SessionContext );
		workspace.refresh( OperationSubmitter.blocking() ).join();

		assertBookNumberIsEqualsTo( NUMBER_OF_BOOKS, tenant1SessionContext );
		assertBookNumberIsEqualsTo( NUMBER_OF_BOOKS, tenant2SessionContext );

		workspace.mergeSegments( OperationSubmitter.blocking() ).join();
		assertBookNumberIsEqualsTo( NUMBER_OF_BOOKS, tenant1SessionContext );
		assertBookNumberIsEqualsTo( NUMBER_OF_BOOKS, tenant2SessionContext );

		workspace.purge( Collections.emptySet(), OperationSubmitter.blocking() ).join();
		workspace.flush( OperationSubmitter.blocking() ).join();
		workspace.refresh( OperationSubmitter.blocking() ).join();

		// check that only TENANT_1 is affected by the purge
		assertBookNumberIsEqualsTo( 0, tenant1SessionContext );
		assertBookNumberIsEqualsTo( NUMBER_OF_BOOKS, tenant2SessionContext );
	}

	private void createBookIndexes(StubSession sessionContext) {
		index.bulkIndexer( sessionContext, false ) // No refresh
				.add( NUMBER_OF_BOOKS, i -> documentProvider(
						String.valueOf( i ),
						document -> document.addValue( index.binding().title, "The Lord of the Rings cap. " + i )
				) )
				.join();
	}

	private void assertBookNumberIsEqualsTo(long bookNumber, StubSession sessionContext) {
		SearchQuery<DocumentReference> query = index.createScope().query( sessionContext )
				.where( f -> f.matchAll() )
				.toQuery();

		assertThat( query.fetchTotalHitCount() ).isEqualTo( bookNumber );
	}

	private static class IndexBinding {
		final IndexFieldReference<String> title;

		IndexBinding(IndexSchemaElement root) {
			title = root.field( "title", f -> f.asString() )
					.toReference();
		}
	}
}
