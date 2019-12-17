/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.rule;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.util.common.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.StubBackendBehavior;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.StubDocumentNode;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.StubIndexSchemaNode;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.StubDocumentWork;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.StubIndexScopeWork;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.StubSearchWork;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import org.easymock.Capture;

public class BackendMock implements TestRule {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final String backendName;
	private final VerifyingStubBackendBehavior behaviorMock = new VerifyingStubBackendBehavior();

	public BackendMock(String backendName) {
		this.backendName = backendName;
	}

	public String getBackendName() {
		return backendName;
	}

	@Override
	public Statement apply(Statement base, Description description) {
		return new Statement() {
			@Override
			public void evaluate() throws Throwable {
				setup();
				try {
					base.evaluate();
					verifyExpectationsMet();
				}
				finally {
					resetExpectations();
					tearDown();
				}
			}
		};
	}

	public void resetExpectations() {
		behaviorMock.resetExpectations();
	}

	public void verifyExpectationsMet() {
		behaviorMock.verifyExpectationsMet();
	}

	private void setup() {
		StubBackendBehavior.set( backendName, behaviorMock );
	}

	private void tearDown() {
		StubBackendBehavior.unset( backendName, behaviorMock );
	}

	public void inLenientMode(Runnable action) {
		behaviorMock.setLenient( true );
		try {
			action.run();
		}
		finally {
			behaviorMock.setLenient( false );
		}
	}

	public BackendMock expectFailingField(String indexName, String absoluteFieldPath,
			Supplier<RuntimeException> exceptionSupplier) {
		behaviorMock.setIndexFieldAddBehavior( indexName, absoluteFieldPath, () -> {
			throw exceptionSupplier.get();
		} );
		return this;
	}

	public BackendMock expectSchema(String indexName, Consumer<StubIndexSchemaNode.Builder> contributor) {
		return expectSchema( indexName, contributor, Capture.newInstance() );
	}

	public BackendMock expectSchema(String indexName, Consumer<StubIndexSchemaNode.Builder> contributor,
			Capture<StubIndexSchemaNode> capture) {
		CallQueue<PushSchemaCall> callQueue = behaviorMock.getPushSchemaCalls( indexName );
		StubIndexSchemaNode.Builder builder = StubIndexSchemaNode.schema();
		contributor.accept( builder );
		callQueue.expectOutOfOrder( new PushSchemaCall( indexName, builder.build(), capture ) );
		return this;
	}

	public BackendMock expectAnySchema(String indexName) {
		CallQueue<PushSchemaCall> callQueue = behaviorMock.getPushSchemaCalls( indexName );
		callQueue.expectOutOfOrder( new PushSchemaCall( indexName, null ) );
		return this;
	}

	public DocumentWorkCallListContext expectWorks(String indexName) {
		// Default to force commit and no refresh, which is what the mapper should use by default
		return expectWorks( indexName, DocumentCommitStrategy.FORCE, DocumentRefreshStrategy.NONE );
	}

	public DocumentWorkCallListContext expectWorks(String indexName,
			DocumentCommitStrategy commitStrategy,
			DocumentRefreshStrategy refreshStrategy) {
		CallQueue<DocumentWorkCall> callQueue = behaviorMock.getDocumentWorkCalls( indexName );
		return new DocumentWorkCallListContext(
				indexName,
				commitStrategy, refreshStrategy,
				callQueue::expectInOrder
		);
	}

	public DocumentWorkCallListContext expectWorksAnyOrder(String indexName,
			DocumentCommitStrategy commitStrategy,
			DocumentRefreshStrategy refreshStrategy) {
		CallQueue<DocumentWorkCall> callQueue = behaviorMock.getDocumentWorkCalls( indexName );
		return new DocumentWorkCallListContext(
				indexName,
				commitStrategy, refreshStrategy,
				callQueue::expectOutOfOrder
		);
	}

	public IndexScopeWorkCallListContext expectIndexScopeWorks(String indexName) {
		return expectIndexScopeWorks( indexName , null );
	}

	public IndexScopeWorkCallListContext expectIndexScopeWorks(String indexName, String tenantId) {
		return expectIndexScopeWorks( Collections.singletonList( indexName ), tenantId );
	}

	public IndexScopeWorkCallListContext expectIndexScopeWorks(Collection<String> indexNames, String tenantId) {
		CallQueue<IndexScopeWorkCall> callQueue = behaviorMock.getIndexScopeWorkCalls();
		return new IndexScopeWorkCallListContext(
				new LinkedHashSet<>( indexNames ), tenantId,
				callQueue::expectInOrder
		);
	}

	public BackendMock expectSearchReferences(Collection<String> indexNames, Consumer<StubSearchWork.Builder> contributor,
			StubSearchWorkBehavior<DocumentReference> behavior) {
		return expectSearch( indexNames, contributor, StubSearchWork.ResultType.REFERENCES, behavior );
	}

	public BackendMock expectSearchObjects(Collection<String> indexNames, Consumer<StubSearchWork.Builder> contributor,
			StubSearchWorkBehavior<DocumentReference> behavior) {
		return expectSearch( indexNames, contributor, StubSearchWork.ResultType.OBJECTS, behavior );
	}

	public BackendMock expectSearchProjections(Collection<String> indexNames, Consumer<StubSearchWork.Builder> contributor,
			StubSearchWorkBehavior<List<?>> behavior) {
		return expectSearch( indexNames, contributor, StubSearchWork.ResultType.PROJECTIONS, behavior );
	}

	public BackendMock expectSearchProjection(Collection<String> indexNames, Consumer<StubSearchWork.Builder> contributor,
			StubSearchWorkBehavior<?> behavior) {
		return expectSearch( indexNames, contributor, StubSearchWork.ResultType.PROJECTIONS, behavior );
	}

	private BackendMock expectSearch(Collection<String> indexNames, Consumer<StubSearchWork.Builder> contributor,
			StubSearchWork.ResultType resultType, StubSearchWorkBehavior<?> behavior) {
		CallQueue<SearchWorkCall<?>> callQueue = behaviorMock.getSearchWorkCalls();
		StubSearchWork.Builder builder = StubSearchWork.builder( resultType );
		contributor.accept( builder );
		callQueue.expectInOrder( new SearchWorkCall<>( new LinkedHashSet<>( indexNames ), builder.build(), behavior ) );
		return this;
	}

	public BackendMock expectCount(Collection<String> indexNames, long expectedResult) {
		CallQueue<CountWorkCall> callQueue = behaviorMock.getCountWorkCalls();
		callQueue.expectInOrder( new CountWorkCall( new LinkedHashSet<>( indexNames ), expectedResult ) );
		return this;
	}

	public class DocumentWorkCallListContext {
		private final String indexName;
		private final DocumentCommitStrategy commitStrategyForDocumentWorks;
		private final DocumentRefreshStrategy refreshStrategyForDocumentWorks;
		private final Consumer<DocumentWorkCall> expectationConsumer;
		private final List<StubDocumentWork> works = new ArrayList<>();

		private DocumentWorkCallListContext(String indexName,
				DocumentCommitStrategy commitStrategyForDocumentWorks,
				DocumentRefreshStrategy refreshStrategyForDocumentWorks,
				Consumer<DocumentWorkCall> expectationConsumer) {
			this.indexName = indexName;
			this.commitStrategyForDocumentWorks = commitStrategyForDocumentWorks;
			this.refreshStrategyForDocumentWorks = refreshStrategyForDocumentWorks;
			this.expectationConsumer = expectationConsumer;
		}

		public DocumentWorkCallListContext add(Consumer<StubDocumentWork.Builder> contributor) {
			return documentWork( StubDocumentWork.Type.ADD, contributor );
		}

		public DocumentWorkCallListContext add(String id, Consumer<StubDocumentNode.Builder> documentContributor) {
			return documentWork( StubDocumentWork.Type.ADD, id, documentContributor );
		}

		public DocumentWorkCallListContext update(Consumer<StubDocumentWork.Builder> contributor) {
			return documentWork( StubDocumentWork.Type.UPDATE, contributor );
		}

		public DocumentWorkCallListContext update(String id, Consumer<StubDocumentNode.Builder> documentContributor) {
			return documentWork( StubDocumentWork.Type.UPDATE, id, documentContributor );
		}

		public DocumentWorkCallListContext delete(String id) {
			return documentWork( StubDocumentWork.Type.DELETE, b -> b.identifier( id ) );
		}

		public DocumentWorkCallListContext delete(Consumer<StubDocumentWork.Builder> contributor) {
			return documentWork( StubDocumentWork.Type.DELETE, contributor );
		}

		DocumentWorkCallListContext documentWork(StubDocumentWork.Type type,
				Consumer<StubDocumentWork.Builder> contributor) {
			StubDocumentWork.Builder builder = StubDocumentWork.builder( type );
			contributor.accept( builder );
			builder.commit( commitStrategyForDocumentWorks );
			builder.refresh( refreshStrategyForDocumentWorks );
			return work( builder.build() );
		}

		DocumentWorkCallListContext documentWork(StubDocumentWork.Type type, String id,
				Consumer<StubDocumentNode.Builder> documentContributor) {
			return documentWork( type, b -> {
				b.identifier( id );
				StubDocumentNode.Builder documentBuilder = StubDocumentNode.document();
				documentContributor.accept( documentBuilder );
				b.document( documentBuilder.build() );
			} );
		}

		public DocumentWorkCallListContext work(StubDocumentWork work) {
			works.add( work );
			return this;
		}

		public BackendMock processedThenExecuted(CompletableFuture<?> future) {
			log.debugf( "Expecting %d works to be prepared, then executed", works.size() );
			// First expect all works to be prepared, then expect all works to be executed
			works.stream()
					.map( work -> new DocumentWorkCall( indexName, DocumentWorkCall.WorkPhase.PROCESS, work ) )
					.forEach( expectationConsumer );
			works.stream()
					.map( work -> new DocumentWorkCall( indexName, DocumentWorkCall.WorkPhase.EXECUTE, work, future ) )
					.forEach( expectationConsumer );
			return BackendMock.this;
		}

		public BackendMock processedThenExecuted() {
			return processedThenExecuted( CompletableFuture.completedFuture( null ) );
		}

		public BackendMock executed(CompletableFuture<?> future) {
			log.debugf( "Expecting %d works to be executed", works.size() );
			works.stream()
					.map( work -> new DocumentWorkCall( indexName, DocumentWorkCall.WorkPhase.EXECUTE, work, future ) )
					.forEach( expectationConsumer );
			return BackendMock.this;
		}

		public BackendMock executed() {
			return executed( CompletableFuture.completedFuture( null ) );
		}

		public BackendMock processed() {
			log.debugf( "Expecting %d works to be prepared", works.size() );
			works.stream()
					.map( work -> new DocumentWorkCall( indexName, DocumentWorkCall.WorkPhase.PROCESS, work ) )
					.forEach( expectationConsumer );
			return BackendMock.this;
		}

		public BackendMock discarded() {
			log.debugf( "Expecting %d works to be discarded", works.size() );
			works.stream()
					.map( work -> new DocumentWorkCall( indexName, DocumentWorkCall.WorkPhase.DISCARD, work ) )
					.forEach( expectationConsumer );
			return BackendMock.this;
		}
	}

	public class IndexScopeWorkCallListContext {
		private final Set<String> indexNames;
		private final String tenantIdentifier;
		private final Consumer<IndexScopeWorkCall> expectationConsumer;

		private IndexScopeWorkCallListContext(Set<String> indexNames,
				String tenantIdentifier,
				Consumer<IndexScopeWorkCall> expectationConsumer) {
			this.indexNames = indexNames;
			this.tenantIdentifier = tenantIdentifier;
			this.expectationConsumer = expectationConsumer;
		}

		public IndexScopeWorkCallListContext forceMerge() {
			return indexScopeWork( StubIndexScopeWork.Type.FORCE_MERGE );
		}

		public IndexScopeWorkCallListContext forceMerge(CompletableFuture<?> future) {
			return indexScopeWork( StubIndexScopeWork.Type.FORCE_MERGE, future );
		}

		public IndexScopeWorkCallListContext purge() {
			return indexScopeWork( StubIndexScopeWork.Type.PURGE );
		}

		public IndexScopeWorkCallListContext purge(CompletableFuture<?> future) {
			return indexScopeWork( StubIndexScopeWork.Type.PURGE, future );
		}

		public IndexScopeWorkCallListContext flush() {
			return indexScopeWork( StubIndexScopeWork.Type.FLUSH );
		}

		public IndexScopeWorkCallListContext flush(CompletableFuture<?> future) {
			return indexScopeWork( StubIndexScopeWork.Type.FLUSH, future );
		}

		public IndexScopeWorkCallListContext indexScopeWork(StubIndexScopeWork.Type type) {
			return indexScopeWork( type, CompletableFuture.completedFuture( null ) );
		}

		public IndexScopeWorkCallListContext indexScopeWork(StubIndexScopeWork.Type type, CompletableFuture<?> future) {
			StubIndexScopeWork work = StubIndexScopeWork.builder( type ).build();
			expectationConsumer.accept( new IndexScopeWorkCall( indexNames, work, future ) );
			return this;
		}
	}

}
