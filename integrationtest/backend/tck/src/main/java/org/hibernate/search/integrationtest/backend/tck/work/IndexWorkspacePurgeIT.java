/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.work;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.backend.work.execution.spi.IndexWorkspace;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckBackendAccessor;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;

public class IndexWorkspacePurgeIT extends AbstractIndexWorkspaceSimpleOperationIT {

	@Override
	protected void ensureOperationsFail(TckBackendAccessor accessor, String indexName) {
		accessor.ensureIndexingOperationsFail( indexName );
	}

	@Override
	protected CompletableFuture<?> executeAsync(IndexWorkspace workspace) {
		return workspace.purge( Collections.emptySet(), OperationSubmitter.rejecting() );
	}

	@Override
	protected void afterInitData(StubMappedIndex index) {
		// Make sure to flush the index, otherwise the test won't fail as expected with Lucene,
		// probably because the index writer optimizes purges when changes are not committed yet.
		index.createWorkspace().flush( OperationSubmitter.blocking() ).join();
	}

	@Override
	protected void assertPreconditions(StubMappedIndex index) {
		index.createWorkspace().refresh( OperationSubmitter.blocking() ).join();
		long count = index.createScope().query().where( f -> f.matchAll() )
				.fetchTotalHitCount();
		assertThat( count ).isGreaterThan( 0 );
	}

	@Override
	protected void assertSuccess(StubMappedIndex index) {
		index.createWorkspace().refresh( OperationSubmitter.blocking() ).join();
		long count = index.createScope().query().where( f -> f.matchAll() )
				.fetchTotalHitCount();
		assertThat( count ).isEqualTo( 0 );
	}
}
