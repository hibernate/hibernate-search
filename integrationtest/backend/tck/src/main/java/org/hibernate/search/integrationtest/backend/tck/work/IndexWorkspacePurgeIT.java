/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.work;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assume.assumeTrue;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.engine.backend.work.execution.spi.IndexWorkspace;
import org.hibernate.search.engine.backend.work.execution.spi.UnsupportedOperationBehavior;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckBackendAccessor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckConfiguration;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;

import org.junit.Before;

public class IndexWorkspacePurgeIT extends AbstractIndexWorkspaceSimpleOperationIT {

	@Before
	public void checkAssumptions() {
		assumeTrue(
				"This test only makes sense if the backend supports explicit purge",
				TckConfiguration.get().getBackendFeatures().supportsExplicitPurge()
		);
	}

	@Override
	protected void ensureOperationsFail(TckBackendAccessor accessor, String indexName) {
		accessor.ensureIndexingOperationsFail( indexName );
	}

	@Override
	protected CompletableFuture<?> executeAsync(IndexWorkspace workspace) {
		return workspace.purge( Collections.emptySet(), OperationSubmitter.rejecting(),
				UnsupportedOperationBehavior.FAIL );
	}

	@Override
	protected void afterInitData(StubMappedIndex index) {
		if ( TckConfiguration.get().getBackendFeatures().supportsExplicitFlush() ) {
			// Make sure to flush the index, otherwise the test won't fail as expected with Lucene,
			// probably because the index writer optimizes purges when changes are not committed yet.
			index.createWorkspace().flush( OperationSubmitter.blocking(), UnsupportedOperationBehavior.IGNORE ).join();
		}
	}

	@Override
	protected void assertPreconditions(StubMappedIndex index) {
		index.searchAfterIndexChanges( () -> {
			assertThat( index.query().where( f -> f.matchAll() ).fetchTotalHitCount() )
					.isGreaterThan( 0 );
		} );
	}

	@Override
	protected void assertSuccess(StubMappedIndex index) {
		index.searchAfterIndexChanges( () -> {
			assertThat( index.query().where( f -> f.matchAll() ).fetchTotalHitCount() )
					.isEqualTo( 0 );
		} );
	}
}
