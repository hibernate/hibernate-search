/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.work;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils.documentProvider;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.Collections;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.engine.backend.work.execution.spi.IndexWorkspace;
import org.hibernate.search.engine.backend.work.execution.spi.UnsupportedOperationBehavior;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckBackendHelper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckConfiguration;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubSession;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Verify that the work executor operations:
 * {@link IndexWorkspace#mergeSegments(OperationSubmitter, UnsupportedOperationBehavior)},
 * {@link IndexWorkspace#purge(java.util.Set, OperationSubmitter, UnsupportedOperationBehavior)},
 * {@link IndexWorkspace#flush(OperationSubmitter, UnsupportedOperationBehavior)},
 * {@link IndexWorkspace#refresh(OperationSubmitter, UnsupportedOperationBehavior)}
 * work properly, in every backends.
 */
class IndexWorkspaceIT {

	private static final String TENANT_1 = "tenant1";
	private static final String TENANT_2 = "tenant2";

	private static final int NUMBER_OF_BOOKS = 200;

	@RegisterExtension
	public final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	@RegisterExtension
	public final SearchSetupHelper multiTenancySetupHelper = SearchSetupHelper.create();

	private final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	@BeforeEach
	void initSessionContexts() {
	}

	@BeforeEach
	public void checkAssumptions() {
		assumeTrue(
				TckConfiguration.get().getBackendFeatures().supportsExplicitPurge()
						&& TckConfiguration.get().getBackendFeatures().supportsExplicitMergeSegments()
						&& TckConfiguration.get().getBackendFeatures().supportsExplicitFlush()
						&& TckConfiguration.get().getBackendFeatures().supportsExplicitRefresh(),
				"This test only makes sense if the backend supports explicit purge, mergeSegments, flush and refresh"
		);
	}

	@Test
	void runMergeSegmentsPurgeAndFlushAndRefreshInSequence() {
		setupHelper.start().withIndex( index ).setup();
		StubSession noTenantSessionContext = index.mapping().session();

		// Do not provide a tenant
		IndexWorkspace workspace = index.createWorkspace();
		createBookIndexes( noTenantSessionContext );

		workspace.refresh( OperationSubmitter.blocking(), UnsupportedOperationBehavior.FAIL ).join();
		assertBookNumberIsEqualsTo( NUMBER_OF_BOOKS, noTenantSessionContext );

		workspace.mergeSegments( OperationSubmitter.blocking(), UnsupportedOperationBehavior.FAIL ).join();
		assertBookNumberIsEqualsTo( NUMBER_OF_BOOKS, noTenantSessionContext );

		// purge without providing a tenant
		workspace.purge( Collections.emptySet(), OperationSubmitter.blocking(), UnsupportedOperationBehavior.FAIL )
				.join();
		workspace.flush( OperationSubmitter.blocking(), UnsupportedOperationBehavior.FAIL ).join();
		workspace.refresh( OperationSubmitter.blocking(), UnsupportedOperationBehavior.FAIL ).join();

		assertBookNumberIsEqualsTo( 0, noTenantSessionContext );
	}

	@Test
	void runMergeSegmentsPurgeAndFlushAndRefreshWithMultiTenancy() {
		multiTenancySetupHelper.start( TckBackendHelper::createMultiTenancyBackendSetupStrategy )
				.withIndex( index ).withMultiTenancy().setup();
		StubSession tenant1SessionContext = index.mapping().session( TENANT_1 );
		StubSession tenant2SessionContext = index.mapping().session( TENANT_2 );

		// Do provide a tenant ID
		IndexWorkspace workspace = index.createWorkspace( tenant1SessionContext );

		createBookIndexes( tenant1SessionContext );
		createBookIndexes( tenant2SessionContext );
		workspace.refresh( OperationSubmitter.blocking(), UnsupportedOperationBehavior.FAIL ).join();

		assertBookNumberIsEqualsTo( NUMBER_OF_BOOKS, tenant1SessionContext );
		assertBookNumberIsEqualsTo( NUMBER_OF_BOOKS, tenant2SessionContext );

		workspace.mergeSegments( OperationSubmitter.blocking(), UnsupportedOperationBehavior.FAIL ).join();
		assertBookNumberIsEqualsTo( NUMBER_OF_BOOKS, tenant1SessionContext );
		assertBookNumberIsEqualsTo( NUMBER_OF_BOOKS, tenant2SessionContext );

		workspace.purge( Collections.emptySet(), OperationSubmitter.blocking(), UnsupportedOperationBehavior.FAIL )
				.join();
		workspace.flush( OperationSubmitter.blocking(), UnsupportedOperationBehavior.FAIL ).join();
		workspace.refresh( OperationSubmitter.blocking(), UnsupportedOperationBehavior.FAIL ).join();

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
