/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.work;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.backend.work.execution.spi.IndexWorkspace;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckBackendAccessor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;

public class IndexWorkspaceRefreshIT extends AbstractIndexWorkspaceSimpleOperationIT {

	public IndexWorkspaceRefreshIT() {
		super( new SearchSetupHelper( helper -> helper.createRarePeriodicRefreshBackendSetupStrategy() ) );
	}

	@Override
	protected void ensureOperationsFail(TckBackendAccessor accessor, String indexName) {
		accessor.ensureFlushMergeRefreshOperationsFail( indexName );
	}

	@Override
	protected CompletableFuture<?> executeAsync(IndexWorkspace workspace) {
		return workspace.refresh( OperationSubmitter.rejecting() );
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
