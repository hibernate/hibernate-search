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

import org.hibernate.search.engine.backend.common.spi.EntityReferenceFactory;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlan;
import org.hibernate.search.engine.backend.work.execution.spi.DocumentContributor;
import org.hibernate.search.engine.backend.work.execution.spi.DocumentReferenceProvider;
import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.StubBackendBehavior;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlanExecutionReport;
import org.hibernate.search.util.common.impl.Futures;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.StubDocumentNode;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.StubDocumentWork;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.impl.StubDocumentElement;

class StubIndexIndexingPlan<R> implements IndexIndexingPlan<R> {
	private final String indexName;
	private final String typeName;
	private final StubBackendBehavior behavior;
	private final BackendSessionContext sessionContext;
	private final EntityReferenceFactory<R> entityReferenceFactory;
	private final DocumentCommitStrategy commitStrategy;
	private final DocumentRefreshStrategy refreshStrategy;

	private final List<StubDocumentWork> works = new ArrayList<>();

	private int preparedIndex = 0;

	StubIndexIndexingPlan(String indexName, String typeName,
			StubBackendBehavior behavior,
			BackendSessionContext sessionContext,
			EntityReferenceFactory<R> entityReferenceFactory,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy) {
		this.indexName = indexName;
		this.typeName = typeName;
		this.sessionContext = sessionContext;
		this.behavior = behavior;
		this.entityReferenceFactory = entityReferenceFactory;
		this.commitStrategy = commitStrategy;
		this.refreshStrategy = refreshStrategy;
	}

	@Override
	public void add(DocumentReferenceProvider documentReferenceProvider,
			DocumentContributor documentContributor) {
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
			DocumentContributor documentContributor) {
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
		for ( StubDocumentWork work : works.subList( preparedIndex, works.size() ) ) {
			behavior.processDocumentWork( indexName, work );
		}
		preparedIndex = works.size();
	}

	@Override
	public CompletableFuture<IndexIndexingPlanExecutionReport<R>> executeAndReport() {
		process();
		List<StubDocumentWork> worksToExecute = new ArrayList<>( works );
		works.clear();
		preparedIndex = 0;
		CompletableFuture<?>[] workFutures = worksToExecute.stream()
				.map( work -> behavior.executeDocumentWork( indexName, work ) )
				.toArray( CompletableFuture<?>[]::new );
		return CompletableFuture.allOf( workFutures )
				.handle( Futures.handler( (ignored1, ignored2) -> {
					// The throwable is ignored, because it comes from a work future and we'll address this below.
					return buildResult( worksToExecute, workFutures );
				} ) );
	}

	private IndexIndexingPlanExecutionReport<R> buildResult(List<StubDocumentWork> worksToExecute,
			CompletableFuture<?>[] finishedWorkFutures) {
		IndexIndexingPlanExecutionReport.Builder<R> builder = IndexIndexingPlanExecutionReport.builder();
		for ( int i = 0; i < finishedWorkFutures.length; i++ ) {
			CompletableFuture<?> future = finishedWorkFutures[i];
			if ( future.isCompletedExceptionally() ) {
				builder.throwable( Futures.getThrowableNow( future ) );
				StubDocumentWork work = worksToExecute.get( i );
				builder.failingEntityReference(
						entityReferenceFactory.createEntityReference( typeName, work.getEntityIdentifier() )
				);
			}
		}
		return builder.build();
	}

	@Override
	public void discard() {
		for ( StubDocumentWork work : works ) {
			behavior.discardDocumentWork( indexName, work );
		}
		works.clear();
	}

	private void populate(StubDocumentWork.Builder builder, DocumentReferenceProvider documentReferenceProvider) {
		builder.tenantIdentifier( sessionContext.getTenantIdentifier() );
		builder.identifier( documentReferenceProvider.getIdentifier() );
		builder.routingKey( documentReferenceProvider.getRoutingKey() );
		builder.entityIdentifier( documentReferenceProvider.getEntityIdentifier() );
	}

	private void addWork(StubDocumentWork work) {
		works.add( work );
	}
}
