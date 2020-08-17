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

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.spi.BackendBuildContext;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.StubDocumentNode;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.StubIndexSchemaNode;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.StubDocumentWork;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.StubIndexScaleWork;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.StubSchemaManagementWork;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.impl.StubBackendFactory;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.StubSearchWork;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import org.easymock.Capture;

public class BackendMock implements TestRule {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final VerifyingStubBackendBehavior backendBehavior = new VerifyingStubBackendBehavior();

	private boolean started = false;

	@Override
	public Statement apply(Statement base, Description description) {
		return new Statement() {
			@Override
			public void evaluate() throws Throwable {
				started = true;
				try {
					base.evaluate();
					verifyExpectationsMet();
				}
				finally {
					resetExpectations();
					started = false;
				}
			}
		};
	}

	public StubBackendFactory factory() {
		return new StubBackendFactory( backendBehavior );
	}

	public void resetExpectations() {
		backendBehavior().resetExpectations();
	}

	public void verifyExpectationsMet() {
		backendBehavior().verifyExpectationsMet();
	}

	public void inLenientMode(Runnable action) {
		backendBehavior().setLenient( true );
		try {
			action.run();
		}
		finally {
			backendBehavior().setLenient( false );
		}
	}

	public BackendMock onCreate(Consumer<BackendBuildContext> behavior) {
		backendBehavior().addCreateBackendBehavior( context -> {
			behavior.accept( context );
			return null;
		} );
		return this;
	}

	public BackendMock onStop(Runnable behavior) {
		backendBehavior().addStopBackendBehavior( () -> {
			behavior.run();
			return null;
		} );
		return this;
	}

	public BackendMock expectFailingField(String indexName, String absoluteFieldPath,
			Supplier<RuntimeException> exceptionSupplier) {
		backendBehavior().setIndexFieldAddBehavior( indexName, absoluteFieldPath, () -> {
			throw exceptionSupplier.get();
		} );
		return this;
	}

	public BackendMock expectSchema(String indexName, Consumer<StubIndexSchemaNode.Builder> contributor) {
		return expectSchema( indexName, contributor, Capture.newInstance() );
	}

	public BackendMock expectSchema(String indexName, Consumer<StubIndexSchemaNode.Builder> contributor,
			Capture<StubIndexSchemaNode> capture) {
		CallQueue<SchemaDefinitionCall> callQueue = backendBehavior().getSchemaDefinitionCalls( indexName );
		StubIndexSchemaNode.Builder builder = StubIndexSchemaNode.schema();
		contributor.accept( builder );
		callQueue.expectOutOfOrder( new SchemaDefinitionCall( indexName, builder.build(), capture ) );
		return this;
	}

	public BackendMock expectAnySchema(String indexName) {
		CallQueue<SchemaDefinitionCall> callQueue = backendBehavior().getSchemaDefinitionCalls( indexName );
		callQueue.expectOutOfOrder( new SchemaDefinitionCall( indexName, null ) );
		return this;
	}

	public SchemaManagementWorkCallListContext expectSchemaManagementWorks(String indexName) {
		CallQueue<SchemaManagementWorkCall> callQueue = backendBehavior().getSchemaManagementWorkCalls( indexName );
		return new SchemaManagementWorkCallListContext(
				indexName,
				callQueue::expectInOrder
		);
	}

	public DocumentWorkCallListContext expectWorks(String indexName) {
		// Default to force commit and no refresh, which is what the mapper should use by default
		return expectWorks( indexName, DocumentCommitStrategy.FORCE, DocumentRefreshStrategy.NONE );
	}

	public DocumentWorkCallListContext expectWorks(String indexName,
			DocumentCommitStrategy commitStrategy,
			DocumentRefreshStrategy refreshStrategy) {
		CallQueue<DocumentWorkCall> callQueue = backendBehavior().getDocumentWorkCalls( indexName );
		return new DocumentWorkCallListContext(
				indexName,
				commitStrategy, refreshStrategy,
				callQueue::expectInOrder
		);
	}

	public DocumentWorkCallListContext expectWorksAnyOrder(String indexName,
			DocumentCommitStrategy commitStrategy,
			DocumentRefreshStrategy refreshStrategy) {
		CallQueue<DocumentWorkCall> callQueue = backendBehavior().getDocumentWorkCalls( indexName );
		return new DocumentWorkCallListContext(
				indexName,
				commitStrategy, refreshStrategy,
				callQueue::expectOutOfOrder
		);
	}

	public IndexScaleWorkCallListContext expectIndexScaleWorks(String indexName) {
		return expectIndexScaleWorks( indexName , null );
	}

	public IndexScaleWorkCallListContext expectIndexScaleWorks(String indexName, String tenantId) {
		CallQueue<IndexScaleWorkCall> callQueue = backendBehavior().getIndexScaleWorkCalls();
		return new IndexScaleWorkCallListContext(
				indexName, tenantId,
				callQueue::expectInOrder
		);
	}

	public BackendMock expectSearchReferences(Collection<String> indexNames, Consumer<StubSearchWork.Builder> contributor,
			StubSearchWorkBehavior<DocumentReference> behavior) {
		return expectSearch( indexNames, contributor, StubSearchWork.ResultType.REFERENCES, behavior );
	}

	public BackendMock expectSearchObjects(String indexName, StubSearchWorkBehavior<DocumentReference> behavior) {
		return expectSearch( Collections.singleton( indexName ), ignored -> { },
				StubSearchWork.ResultType.OBJECTS, behavior );
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
		CallQueue<SearchWorkCall<?>> callQueue = backendBehavior().getSearchWorkCalls();
		StubSearchWork.Builder builder = StubSearchWork.builder( resultType );
		contributor.accept( builder );
		callQueue.expectInOrder( new SearchWorkCall<>( new LinkedHashSet<>( indexNames ), builder.build(), behavior ) );
		return this;
	}

	public BackendMock expectCount(Collection<String> indexNames, long expectedResult) {
		CallQueue<CountWorkCall> callQueue = backendBehavior().getCountWorkCalls();
		callQueue.expectInOrder( new CountWorkCall( new LinkedHashSet<>( indexNames ), expectedResult ) );
		return this;
	}

	private VerifyingStubBackendBehavior backendBehavior() {
		if ( !started ) {
			throw new AssertionFailure( "The backend mock was not configured as a JUnit @Rule/@ClassRule,"
					+ " or its statement wrapper hasn't started executing yet,"
					+ " or its statement wrapper has finished executing."
					+ " Double check the @Rule/@ClassRule annotations and the execution order of rules." );
		}
		return backendBehavior;
	}

	public BackendMock expectScrollObjects(Collection<String> indexNames, int chunkSize,
			Consumer<StubSearchWork.Builder> contributor) {
		return expectScroll( indexNames, contributor, StubSearchWork.ResultType.OBJECTS, chunkSize );
	}

	public BackendMock expectScrollProjections(Collection<String> indexNames, int chunkSize,
			Consumer<StubSearchWork.Builder> contributor) {
		return expectScroll( indexNames, contributor, StubSearchWork.ResultType.PROJECTIONS, chunkSize );
	}

	public BackendMock expectScrollProjection(Collection<String> indexNames, int chunkSize,
			Consumer<StubSearchWork.Builder> contributor) {
		return expectScroll( indexNames, contributor, StubSearchWork.ResultType.PROJECTIONS, chunkSize );
	}

	private BackendMock expectScroll(Collection<String> indexNames, Consumer<StubSearchWork.Builder> contributor,
			StubSearchWork.ResultType resultType, int chunkSize) {
		CallQueue<ScrollWorkCall<?>> callQueue = backendBehavior().getScrollCalls();
		StubSearchWork.Builder builder = StubSearchWork.builder( resultType );
		contributor.accept( builder );
		callQueue.expectInOrder( new ScrollWorkCall<>( new LinkedHashSet<>( indexNames ), builder.build(), chunkSize ) );
		return this;
	}

	public BackendMock expectCloseScroll(Collection<String> indexNames) {
		CallQueue<CloseScrollWorkCall> callQueue = backendBehavior().getCloseScrollCalls();
		callQueue.expectInOrder( new CloseScrollWorkCall( new LinkedHashSet<>( indexNames ) ) );
		return this;
	}

	public BackendMock expectNextScroll(Collection<String> indexNames, StubNextScrollWorkBehavior<?> behavior) {
		CallQueue<NextScrollWorkCall<?>> callQueue = backendBehavior().getNextScrollCalls();
		callQueue.expectInOrder( new NextScrollWorkCall<>( new LinkedHashSet<>( indexNames ), behavior ) );
		return this;
	}

	public static class SchemaManagementWorkCallListContext {
		private final String indexName;
		private final Consumer<SchemaManagementWorkCall> expectationConsumer;

		private SchemaManagementWorkCallListContext(String indexName,
				Consumer<SchemaManagementWorkCall> expectationConsumer) {
			this.indexName = indexName;
			this.expectationConsumer = expectationConsumer;
		}

		public SchemaManagementWorkCallListContext work(StubSchemaManagementWork.Type type) {
			return work( type, CompletableFuture.completedFuture( null ) );
		}

		public SchemaManagementWorkCallListContext work(StubSchemaManagementWork.Type type, CompletableFuture<?> future) {
			return work( type, failureCollector -> future );
		}

		public SchemaManagementWorkCallListContext work(StubSchemaManagementWork.Type type,
				SchemaManagementWorkBehavior behavior) {
			StubSchemaManagementWork work = StubSchemaManagementWork.builder( type )
					.build();
			expectationConsumer.accept( new SchemaManagementWorkCall( indexName, work, behavior ) );
			return this;
		}
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
			works.clear();
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
			works.clear();
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

	public static class IndexScaleWorkCallListContext {
		private final String indexName;
		private final String tenantIdentifier;
		private final Consumer<IndexScaleWorkCall> expectationConsumer;

		private IndexScaleWorkCallListContext(String indexName,
				String tenantIdentifier,
				Consumer<IndexScaleWorkCall> expectationConsumer) {
			this.indexName = indexName;
			this.tenantIdentifier = tenantIdentifier;
			this.expectationConsumer = expectationConsumer;
		}

		public IndexScaleWorkCallListContext mergeSegments() {
			return indexScaleWork( StubIndexScaleWork.Type.MERGE_SEGMENTS );
		}

		public IndexScaleWorkCallListContext mergeSegments(CompletableFuture<?> future) {
			return indexScaleWork( StubIndexScaleWork.Type.MERGE_SEGMENTS, future );
		}

		public IndexScaleWorkCallListContext purge() {
			return indexScaleWork( StubIndexScaleWork.Type.PURGE );
		}

		public IndexScaleWorkCallListContext purge(CompletableFuture<?> future) {
			return indexScaleWork( StubIndexScaleWork.Type.PURGE, future );
		}

		public IndexScaleWorkCallListContext purge(Set<String> routingKeys) {
			return indexScaleWork( StubIndexScaleWork.Type.PURGE, routingKeys );
		}

		public IndexScaleWorkCallListContext purge(Set<String> routingKeys, CompletableFuture<?> future) {
			return indexScaleWork( StubIndexScaleWork.Type.PURGE, routingKeys, future );
		}

		public IndexScaleWorkCallListContext flush() {
			return indexScaleWork( StubIndexScaleWork.Type.FLUSH );
		}

		public IndexScaleWorkCallListContext flush(CompletableFuture<?> future) {
			return indexScaleWork( StubIndexScaleWork.Type.FLUSH, future );
		}

		public IndexScaleWorkCallListContext refresh() {
			return indexScaleWork( StubIndexScaleWork.Type.REFRESH );
		}

		public IndexScaleWorkCallListContext refresh(CompletableFuture<?> future) {
			return indexScaleWork( StubIndexScaleWork.Type.REFRESH, future );
		}

		public IndexScaleWorkCallListContext indexScaleWork(StubIndexScaleWork.Type type) {
			return indexScaleWork( type, Collections.emptySet() );
		}

		public IndexScaleWorkCallListContext indexScaleWork(StubIndexScaleWork.Type type, CompletableFuture<?> future) {
			return indexScaleWork( type, Collections.emptySet(), future );
		}

		public IndexScaleWorkCallListContext indexScaleWork(StubIndexScaleWork.Type type, Set<String> routingKeys) {
			return indexScaleWork( type, routingKeys, CompletableFuture.completedFuture( null ) );
		}

		public IndexScaleWorkCallListContext indexScaleWork(StubIndexScaleWork.Type type, Set<String> routingKeys,
				CompletableFuture<?> future) {
			StubIndexScaleWork work = StubIndexScaleWork.builder( type )
					.tenantIdentifier( tenantIdentifier )
					.routingKeys( routingKeys )
					.build();
			expectationConsumer.accept( new IndexScaleWorkCall( indexName, work, future ) );
			return this;
		}
	}

}
