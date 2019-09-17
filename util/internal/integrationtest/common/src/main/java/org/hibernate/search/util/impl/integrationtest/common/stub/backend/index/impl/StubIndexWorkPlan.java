/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.backend.work.execution.spi.IndexWorkPlan;
import org.hibernate.search.engine.backend.work.execution.spi.DocumentContributor;
import org.hibernate.search.engine.backend.work.execution.spi.DocumentReferenceProvider;
import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.StubDocumentNode;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.StubDocumentWork;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.impl.StubDocumentElement;

class StubIndexWorkPlan implements IndexWorkPlan<StubDocumentElement> {
	private final StubIndexManager indexManager;
	private final BackendSessionContext sessionContext;
	private final DocumentCommitStrategy commitStrategy;
	private final DocumentRefreshStrategy refreshStrategy;

	private final List<StubDocumentWork> works = new ArrayList<>();

	private int preparedIndex = 0;

	StubIndexWorkPlan(StubIndexManager indexManager, BackendSessionContext sessionContext,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy) {
		this.sessionContext = sessionContext;
		this.indexManager = indexManager;
		this.commitStrategy = commitStrategy;
		this.refreshStrategy = refreshStrategy;
	}

	@Override
	public void add(DocumentReferenceProvider documentReferenceProvider,
			DocumentContributor<StubDocumentElement> documentContributor) {
		StubDocumentWork.Builder builder = StubDocumentWork.builder( StubDocumentWork.Type.ADD );
		populate( builder, documentReferenceProvider );
		StubDocumentNode.Builder documentBuilder = StubDocumentNode.document();
		StubDocumentElement documentElement = new StubDocumentElement( documentBuilder );
		documentContributor.contribute( documentElement );
		builder.document( documentBuilder.build() );
		builder.commit( commitStrategy );
		builder.refresh( refreshStrategy );
		addWork( builder.build() );
	}

	@Override
	public void update(DocumentReferenceProvider documentReferenceProvider,
			DocumentContributor<StubDocumentElement> documentContributor) {
		StubDocumentWork.Builder builder = StubDocumentWork.builder( StubDocumentWork.Type.UPDATE );
		populate( builder, documentReferenceProvider );
		StubDocumentNode.Builder documentBuilder = StubDocumentNode.document();
		StubDocumentElement documentElement = new StubDocumentElement( documentBuilder );
		documentContributor.contribute( documentElement );
		builder.document( documentBuilder.build() );
		builder.commit( commitStrategy );
		builder.refresh( refreshStrategy );
		addWork( builder.build() );
	}

	@Override
	public void delete(DocumentReferenceProvider documentReferenceProvider) {
		StubDocumentWork.Builder builder = StubDocumentWork.builder( StubDocumentWork.Type.DELETE );
		populate( builder, documentReferenceProvider );
		builder.commit( commitStrategy );
		builder.refresh( refreshStrategy );
		addWork( builder.build() );
	}

	@Override
	public void process() {
		indexManager.process( works.subList( preparedIndex, works.size() ) );
		preparedIndex = works.size();
	}

	@Override
	public CompletableFuture<?> execute() {
		process();
		CompletableFuture<?> future = indexManager.execute( works );
		works.clear();
		preparedIndex = 0;
		return future;
	}

	@Override
	public void discard() {
		indexManager.discard( works );
		works.clear();
	}

	private void populate(StubDocumentWork.Builder builder, DocumentReferenceProvider documentReferenceProvider) {
		builder.tenantIdentifier( sessionContext.getTenantIdentifier() );
		builder.identifier( documentReferenceProvider.getIdentifier() );
		builder.routingKey( documentReferenceProvider.getRoutingKey() );
	}

	private void addWork(StubDocumentWork work) {
		works.add( work );
	}
}
