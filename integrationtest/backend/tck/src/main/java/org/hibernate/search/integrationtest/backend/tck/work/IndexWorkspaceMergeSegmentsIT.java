/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.work;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.backend.work.execution.spi.IndexWorkspace;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckBackendAccessor;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingIndexManager;

public class IndexWorkspaceMergeSegmentsIT extends AbstractIndexWorkspaceSimpleOperationIT {

	@Override
	protected void ensureOperationsFail(TckBackendAccessor accessor, String indexName) {
		accessor.ensureFlushMergeRefreshOperationsFail( indexName );
	}

	@Override
	protected CompletableFuture<?> executeAsync(IndexWorkspace workspace) {
		return workspace.mergeSegments();
	}

	@Override
	protected void assertPreconditions(StubMappingIndexManager indexManager) {
		// No measurable effect: don't assert anything.
	}

	@Override
	protected void assertSuccess(StubMappingIndexManager indexManager) {
		// No measurable effect: don't assert anything.
	}
}
