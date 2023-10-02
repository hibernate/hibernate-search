/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.work;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.engine.backend.work.execution.spi.IndexWorkspace;
import org.hibernate.search.engine.backend.work.execution.spi.UnsupportedOperationBehavior;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckBackendAccessor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckBackendHelper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckConfiguration;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;

import org.junit.jupiter.api.BeforeEach;

class IndexWorkspaceRefreshIT extends AbstractIndexWorkspaceSimpleOperationIT {

	public IndexWorkspaceRefreshIT() {
		super( SearchSetupHelper.create() );
	}

	@Override
	protected SearchSetupHelper.SetupContext startHelper() {
		return setupHelper.start( TckBackendHelper::createRarePeriodicRefreshBackendSetupStrategy );
	}

	@BeforeEach
	public void checkAssumptions() {
		assumeTrue(
				TckConfiguration.get().getBackendFeatures().supportsExplicitRefresh(),
				"This test only makes sense if the backend supports explicit refresh"
		);
	}

	@Override
	protected void ensureOperationsFail(TckBackendAccessor accessor, String indexName) {
		accessor.ensureFlushMergeRefreshOperationsFail( indexName );
	}

	@Override
	protected CompletableFuture<?> executeAsync(IndexWorkspace workspace) {
		return workspace.refresh( OperationSubmitter.rejecting(), UnsupportedOperationBehavior.FAIL );
	}

	@Override
	protected void beforeInitData(StubMappedIndex index) {
		// Make sure index readers are initialized before writing,
		// otherwise the preconditions won't be met.
		index.createScope().query().where( f -> f.matchAll() ).fetchTotalHitCount();
	}

	@Override
	protected void assertPreconditions(StubMappedIndex index) {
		long count = index.createScope().query().where( f -> f.matchAll() )
				.fetchTotalHitCount();
		// Indexes haven't been refreshed yet, so documents added so far are missing.
		assertThat( count ).isEqualTo( 0 );
	}

	@Override
	protected void assertSuccess(StubMappedIndex index) {
		long count = index.createScope().query().where( f -> f.matchAll() )
				.fetchTotalHitCount();
		// After a refresh, documents are visible
		assertThat( count ).isGreaterThan( 0 );
	}
}
