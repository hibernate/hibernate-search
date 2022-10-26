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
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.StubBackendBehavior;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.StubDocumentNode;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.impl.StubDocumentElement;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.StubDocumentWork;

public class StubIndexIndexer implements IndexIndexer {
	private final String indexName;
	private final StubBackendBehavior behavior;
	private final BackendSessionContext sessionContext;

	StubIndexIndexer(String indexName, StubBackendBehavior behavior,
			BackendSessionContext sessionContext) {
		this.indexName = indexName;
		this.behavior = behavior;
		this.sessionContext = sessionContext;
	}

	@Override
	public CompletableFuture<?> add(DocumentReferenceProvider referenceProvider,
			DocumentContributor documentContributor,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy,
			OperationSubmitter operationSubmitter) {
		StubDocumentNode.Builder documentBuilder = StubDocumentNode.document();
		documentContributor.contribute( new StubDocumentElement( documentBuilder ) );

		StubDocumentWork work = StubDocumentWork.builder( StubDocumentWork.Type.ADD )
				.tenantIdentifier( sessionContext.tenantIdentifier() )
				.identifier( referenceProvider.identifier() )
				.routingKey( referenceProvider.routingKey() )
				.document( documentBuilder.build() )
				.commit( commitStrategy )
				.refresh( refreshStrategy )
				.build();
		behavior.createDocumentWork( indexName, work );
		return behavior.executeDocumentWork( indexName, work );
	}

	@Override
	public CompletableFuture<?> addOrUpdate(DocumentReferenceProvider referenceProvider,
			DocumentContributor documentContributor,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy,
			OperationSubmitter operationSubmitter) {
		StubDocumentNode.Builder documentBuilder = StubDocumentNode.document();
		documentContributor.contribute( new StubDocumentElement( documentBuilder ) );

		StubDocumentWork work = StubDocumentWork.builder( StubDocumentWork.Type.ADD_OR_UPDATE )
				.tenantIdentifier( sessionContext.tenantIdentifier() )
				.identifier( referenceProvider.identifier() )
				.routingKey( referenceProvider.routingKey() )
				.document( documentBuilder.build() )
				.commit( commitStrategy )
				.refresh( refreshStrategy )
				.build();
		behavior.createDocumentWork( indexName, work );
		return behavior.executeDocumentWork( indexName, work );
	}

	@Override
	public CompletableFuture<?> delete(DocumentReferenceProvider referenceProvider,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy,
			OperationSubmitter operationSubmitter) {
		StubDocumentWork work = StubDocumentWork.builder( StubDocumentWork.Type.DELETE )
				.tenantIdentifier( sessionContext.tenantIdentifier() )
				.identifier( referenceProvider.identifier() )
				.routingKey( referenceProvider.routingKey() )
				.commit( commitStrategy )
				.refresh( refreshStrategy )
				.build();
		behavior.createDocumentWork( indexName, work );
		return behavior.executeDocumentWork( indexName, work );
	}
}
