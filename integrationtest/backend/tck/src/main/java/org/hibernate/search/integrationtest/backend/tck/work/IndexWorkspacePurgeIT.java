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
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingIndexManager;

public class IndexWorkspacePurgeIT extends AbstractIndexWorkspaceSimpleOperationIT {
	@Override
	protected boolean operationWillFailIfAppliedToDeletedIndex() {
		return true;
	}

	@Override
	protected CompletableFuture<?> executeAsync(IndexWorkspace workspace) {
		return workspace.purge();
	}

	@Override
	protected void assertSuccess(StubMappingIndexManager indexManager) {
		indexManager.createWorkspace().flush().join();
		long count = indexManager.createScope().query().where( f -> f.matchAll() )
				.fetchTotalHitCount();
		assertThat( count ).isEqualTo( 0 );
	}
}
