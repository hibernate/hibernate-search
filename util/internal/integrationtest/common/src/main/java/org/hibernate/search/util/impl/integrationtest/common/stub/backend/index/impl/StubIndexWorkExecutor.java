/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.impl;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.backend.work.execution.spi.IndexWorkExecutor;
import org.hibernate.search.engine.backend.session.spi.DetachedBackendSessionContext;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.StubBackendBehavior;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.StubIndexScopeWork;

class StubIndexWorkExecutor implements IndexWorkExecutor {

	private final String indexName;
	private final StubBackendBehavior behavior;
	private final DetachedBackendSessionContext sessionContext;

	StubIndexWorkExecutor(String indexName, StubBackendBehavior behavior,
			DetachedBackendSessionContext sessionContext) {
		this.indexName = indexName;
		this.behavior = behavior;
		this.sessionContext = sessionContext;
	}

	@Override
	public CompletableFuture<?> optimize() {
		StubIndexScopeWork work = StubIndexScopeWork.builder( StubIndexScopeWork.Type.OPTIMIZE ).build();
		return behavior.executeIndexScopeWork( Collections.singleton( indexName ), work );
	}

	@Override
	public CompletableFuture<?> purge() {
		StubIndexScopeWork work = StubIndexScopeWork.builder( StubIndexScopeWork.Type.PURGE )
				.tenantIdentifier( sessionContext.getTenantIdentifier() )
				.build();
		return behavior.executeIndexScopeWork( Collections.singleton( indexName ), work );
	}

	@Override
	public CompletableFuture<?> flush() {
		StubIndexScopeWork work = StubIndexScopeWork.builder( StubIndexScopeWork.Type.FLUSH ).build();
		return behavior.executeIndexScopeWork( Collections.singleton( indexName ), work );
	}
}
