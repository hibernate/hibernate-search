/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.work;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.engine.backend.work.execution.spi.IndexWorkspace;
import org.hibernate.search.engine.backend.work.execution.spi.UnsupportedOperationBehavior;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckBackendAccessor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckConfiguration;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;

import org.junit.jupiter.api.BeforeEach;

class IndexWorkspaceFlushIT extends AbstractIndexWorkspaceSimpleOperationIT {

	@BeforeEach
	public void checkAssumptions() {
		assumeTrue(
				TckConfiguration.get().getBackendFeatures().supportsExplicitFlush(),
				"This test only makes sense if the backend supports explicit flush"
		);
	}

	@Override
	protected void ensureOperationsFail(TckBackendAccessor accessor, String indexName) {
		accessor.ensureFlushMergeRefreshOperationsFail( indexName );
	}

	@Override
	protected CompletableFuture<?> executeAsync(IndexWorkspace workspace) {
		return workspace.flush( OperationSubmitter.rejecting(), UnsupportedOperationBehavior.FAIL );
	}

	@Override
	protected void assertPreconditions(StubMappedIndex index) {
		// No measurable effect: don't assert anything.
	}

	@Override
	protected void assertSuccess(StubMappedIndex index) {
		// No measurable effect: don't assert anything.
	}
}
