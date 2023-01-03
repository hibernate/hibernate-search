/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.impl;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.backend.work.execution.spi.IndexWorkspace;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.StubBackendBehavior;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.StubIndexScaleWork;

class StubIndexWorkspace implements IndexWorkspace {

	private final String indexName;
	private final StubBackendBehavior behavior;
	private final String tenantId;

	StubIndexWorkspace(String indexName, StubBackendBehavior behavior, String tenantId) {
		this.indexName = indexName;
		this.behavior = behavior;
		this.tenantId = tenantId;
	}

	@Override
	public CompletableFuture<?> mergeSegments(OperationSubmitter operationSubmitter) {
		StubIndexScaleWork work = StubIndexScaleWork.builder( StubIndexScaleWork.Type.MERGE_SEGMENTS )
				// In a real-world backend the operation would cross tenants,
				// because that doesn't matter,
				// but here passing along the tenant identifier
				// makes testing easier.
				.tenantIdentifier( tenantId )
				.build();
		return behavior.executeIndexScaleWork( indexName, work );
	}

	@Override
	public CompletableFuture<?> purge(Set<String> routingKeys, OperationSubmitter operationSubmitter) {
		StubIndexScaleWork work = StubIndexScaleWork.builder( StubIndexScaleWork.Type.PURGE )
				.tenantIdentifier( tenantId )
				.routingKeys( routingKeys )
				.build();
		return behavior.executeIndexScaleWork( indexName, work );
	}

	@Override
	public CompletableFuture<?> flush(OperationSubmitter operationSubmitter) {
		StubIndexScaleWork work = StubIndexScaleWork.builder( StubIndexScaleWork.Type.FLUSH )
				// In a real-world backend the operation would cross tenants,
				// because that doesn't matter,
				// but here passing along the tenant identifier
				// makes testing easier.
				.tenantIdentifier( tenantId )
				.build();
		return behavior.executeIndexScaleWork( indexName, work );
	}

	@Override
	public CompletableFuture<?> refresh(OperationSubmitter operationSubmitter) {
		StubIndexScaleWork work = StubIndexScaleWork.builder( StubIndexScaleWork.Type.REFRESH )
				// In a real-world backend the operation would cross tenants,
				// because that doesn't matter,
				// but here passing along the tenant identifier
				// makes testing easier.
				.tenantIdentifier( tenantId )
				.build();
		return behavior.executeIndexScaleWork( indexName, work );
	}
}
