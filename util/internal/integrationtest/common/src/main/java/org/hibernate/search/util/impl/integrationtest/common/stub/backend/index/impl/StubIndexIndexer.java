/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.impl;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.spi.DocumentContributor;
import org.hibernate.search.engine.backend.work.execution.spi.DocumentReferenceProvider;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexer;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.StubBackendBehavior;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.StubDocumentNode;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.impl.StubDocumentElement;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.StubDocumentWork;

public class StubIndexIndexer implements IndexIndexer<StubDocumentElement> {
	private final String indexName;
	private final StubBackendBehavior behavior;
	private final BackendSessionContext sessionContext;
	private final DocumentCommitStrategy commitStrategy;

	StubIndexIndexer(String indexName, StubBackendBehavior behavior,
			BackendSessionContext sessionContext,
			DocumentCommitStrategy commitStrategy) {
		this.indexName = indexName;
		this.behavior = behavior;
		this.sessionContext = sessionContext;
		this.commitStrategy = commitStrategy;
	}

	@Override
	public CompletableFuture<?> add(DocumentReferenceProvider documentReferenceProvider,
			DocumentContributor<StubDocumentElement> documentContributor) {
		StubDocumentNode.Builder documentBuilder = StubDocumentNode.document();
		documentContributor.contribute( new StubDocumentElement( documentBuilder ) );

		StubDocumentWork work = StubDocumentWork.builder( StubDocumentWork.Type.ADD )
				.tenantIdentifier( sessionContext.getTenantIdentifier() )
				.identifier( documentReferenceProvider.getIdentifier() )
				.routingKey( documentReferenceProvider.getRoutingKey() )
				.document( documentBuilder.build() )
				.commit( commitStrategy )
				.refresh( DocumentRefreshStrategy.NONE )
				.build();

		return behavior.processAndExecuteDocumentWork( indexName, work );
	}
}
