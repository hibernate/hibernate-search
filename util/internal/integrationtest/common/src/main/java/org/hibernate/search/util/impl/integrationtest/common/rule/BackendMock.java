/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.rule;

import static org.hibernate.search.util.common.impl.CollectionHelper.asSetIgnoreNull;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.impl.integrationtest.common.assertion.StubDocumentWorkAssert;
import org.hibernate.search.util.impl.integrationtest.common.stub.StubTreeNodeDiffer;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.BackendMappingHandle;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.StubDocumentNode;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.StubIndexSchemaDataNode;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.impl.StubIndexModel;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.StubDocumentWork;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.StubIndexScaleWork;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.StubSchemaManagementWork;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.impl.StubBackendBuildContext;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.impl.StubBackendFactory;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.impl.StubIndexCreateContext;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.query.impl.StubSearchWork;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class BackendMock implements TestRule {

	private final VerifyingStubBackendBehavior backendBehavior =
			new VerifyingStubBackendBehavior( this::indexingWorkExpectations );

	private volatile boolean started = false;

	private volatile BackendIndexingWorkExpectations indexingWorkExpectations = BackendIndexingWorkExpectations.sync();

	private final Map<String, StubTreeNodeDiffer<StubDocumentNode>> documentDiffers = new ConcurrentHashMap<>();

	@Override
	public Statement apply(Statement base, Description description) {
		return new Statement() {
			@Override
			public void evaluate() throws Throwable {
				started = true;
				try {
					base.evaluate();
					// Workaround for a problem in Hibernate ORM's CustomRunner
					// (used by BytecodeEnhancerRunner in particular)
					// which applies class rules twices, resulting in "started" being false
					// when we get here in the outermost statement...
					if ( started ) {
						verifyExpectationsMet();
					}
				}
				finally {
					if ( started ) {
						resetExpectations();
						started = false;
						backendBehavior.resetBackends();
					}
				}
			}
		};
	}

	public BackendMock ignoreSchema() {
		backendBehavior.ignoreSchema( true );
		return this;
	}

	public BackendMock documentDiffer(String indexName, StubTreeNodeDiffer<StubDocumentNode> differ) {
		documentDiffers.put( indexName, differ );
		return this;
	}

	public StubBackendFactory factory(CompletionStage<BackendMappingHandle> mappingHandlePromise) {
		return new StubBackendFactory( backendBehavior, mappingHandlePromise );
	}

	public void indexingWorkExpectations(BackendIndexingWorkExpectations expectations) {
		indexingWorkExpectations = expectations;
	}

	public BackendIndexingWorkExpectations indexingWorkExpectations() {
		return indexingWorkExpectations;
	}

	public void resetExpectations() {
		backendBehavior().resetExpectations();
	}

	public void verifyExpectationsMet() {
		backendBehavior().verifyExpectationsMet();
	}

	public long remainingExpectedIndexingCount() {
		return backendBehavior().getDocumentWorkExecuteCalls().values().stream()
				.mapToLong( CallQueue::remainingExpectedCallCount )
				.sum();
	}

	public void inLenientMode(Runnable action) {
		backendBehavior().lenient( true );
		try {
			action.run();
		}
		finally {
			backendBehavior().lenient( false );
		}
	}

	public BackendMock onCreate(Consumer<StubBackendBuildContext> behavior) {
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

	public BackendMock onCreateIndex(Consumer<StubIndexCreateContext> behavior) {
		backendBehavior().addCreateIndexBehavior( context -> {
			behavior.accept( context );
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

	public BackendMock expectSchema(String indexName, Consumer<StubIndexSchemaDataNode.Builder> contributor) {
		return expectSchema( indexName, contributor, ignored -> {} );
	}

	public BackendMock expectSchema(String indexName, Consumer<StubIndexSchemaDataNode.Builder> contributor,
			Consumer<StubIndexModel> capture) {
		CallQueue<SchemaDefinitionCall> callQueue = backendBehavior().getSchemaDefinitionCalls( indexName );
		StubIndexSchemaDataNode.Builder builder = StubIndexSchemaDataNode.schema();
		contributor.accept( builder );
		callQueue.expectOutOfOrder( new SchemaDefinitionCall( indexName, builder.build(), capture ) );
		return this;
	}

	public BackendMock expectAnySchema(String indexName) {
		CallQueue<SchemaDefinitionCall> callQueue = backendBehavior().getSchemaDefinitionCalls( indexName );
		callQueue.expectOutOfOrder( new SchemaDefinitionCall( indexName, null, null ) );
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
		return expectWorks( indexName, null );
	}

	public DocumentWorkCallListContext expectWorks(String indexName, String tenantId) {
		// Default to force commit and no refresh, which is what the mapper should use by default
		return expectWorks( indexName, tenantId, DocumentCommitStrategy.FORCE, DocumentRefreshStrategy.NONE );
	}

	public DocumentWorkCallListContext expectWorks(String indexName,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy) {
		return expectWorks( indexName, null, commitStrategy, refreshStrategy );
	}

	public DocumentWorkCallListContext expectWorks(String indexName, String tenantId,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy) {
		return new DocumentWorkCallListContext(
				indexName, tenantId,
				commitStrategy, refreshStrategy,
				DocumentWorkCallKind.CREATE_AND_EXECUTE, CompletableFuture.completedFuture( null )
		);
	}

	public IndexScaleWorkCallListContext expectIndexScaleWorks(String indexName, String... tenantIds) {
		CallQueue<IndexScaleWorkCall> callQueue = backendBehavior().getIndexScaleWorkCalls( indexName );
		return new IndexScaleWorkCallListContext(
				indexName,
				asSetIgnoreNull( tenantIds ),
				callQueue::expectInOrder
		);
	}

	public BackendMock expectSearchReferences(Collection<String> indexNames,
			StubSearchWorkBehavior<DocumentReference> behavior) {
		return expectSearch( indexNames, b -> {}, behavior );
	}

	public BackendMock expectSearchReferences(Collection<String> indexNames, Consumer<StubSearchWork.Builder> contributor,
			StubSearchWorkBehavior<DocumentReference> behavior) {
		return expectSearch( indexNames, contributor, behavior );
	}

	public BackendMock expectSearchIds(Collection<String> indexNames, Consumer<StubSearchWork.Builder> contributor,
			StubSearchWorkBehavior<String> behavior) {
		return expectSearch( indexNames, contributor, behavior );
	}

	public BackendMock expectSearchObjects(String indexName, StubSearchWorkBehavior<DocumentReference> behavior) {
		return expectSearch( Collections.singleton( indexName ), ignored -> {}, behavior );
	}

	public BackendMock expectSearchObjects(Collection<String> indexNames, Consumer<StubSearchWork.Builder> contributor,
			StubSearchWorkBehavior<DocumentReference> behavior) {
		return expectSearch( indexNames, contributor, behavior );
	}

	public BackendMock expectSearchProjection(String indexNames, StubSearchWorkBehavior<?> behavior) {
		return expectSearch( Collections.singleton( indexNames ), ignored -> {}, behavior );
	}

	public BackendMock expectSearchProjection(String indexNames, Consumer<StubSearchWork.Builder> contributor,
			StubSearchWorkBehavior<?> behavior) {
		return expectSearch( Collections.singleton( indexNames ), contributor, behavior );
	}

	public BackendMock expectSearchProjection(Collection<String> indexNames, StubSearchWorkBehavior<?> behavior) {
		return expectSearch( indexNames, ignored -> {}, behavior );
	}

	public BackendMock expectSearchProjection(Collection<String> indexNames, Consumer<StubSearchWork.Builder> contributor,
			StubSearchWorkBehavior<?> behavior) {
		return expectSearch( indexNames, contributor, behavior );
	}

	private BackendMock expectSearch(Collection<String> indexNames, Consumer<StubSearchWork.Builder> contributor,
			StubSearchWorkBehavior<?> behavior) {
		CallQueue<SearchWorkCall<?>> callQueue = backendBehavior().getSearchWorkCalls();
		StubSearchWork.Builder builder = StubSearchWork.builder();
		contributor.accept( builder );
		callQueue.expectInOrder( new SearchWorkCall<>( new LinkedHashSet<>( indexNames ), builder.build(), behavior ) );
		return this;
	}

	public BackendMock expectCount(Collection<String> indexNames, long expectedResult) {
		CallQueue<CountWorkCall> callQueue = backendBehavior().getCountWorkCalls();
		callQueue.expectInOrder( new CountWorkCall( new LinkedHashSet<>( indexNames ), expectedResult ) );
		return this;
	}

	VerifyingStubBackendBehavior backendBehavior() {
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
		return expectScroll( indexNames, contributor, chunkSize );
	}

	public BackendMock expectScrollProjections(Collection<String> indexNames, int chunkSize,
			Consumer<StubSearchWork.Builder> contributor) {
		return expectScroll( indexNames, contributor, chunkSize );
	}

	private BackendMock expectScroll(Collection<String> indexNames, Consumer<StubSearchWork.Builder> contributor,
			int chunkSize) {
		CallQueue<ScrollWorkCall<?>> callQueue = backendBehavior().getScrollCalls();
		StubSearchWork.Builder builder = StubSearchWork.builder();
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

	private enum DocumentWorkCallKind {
		CREATE,
		DISCARD,
		EXECUTE,
		CREATE_AND_DISCARD,
		CREATE_AND_EXECUTE,
		CREATE_AND_EXECUTE_OUT_OF_ORDER;
	}

	public class DocumentWorkCallListContext {
		private final String indexName;
		private final String tenantId;
		private final DocumentCommitStrategy commitStrategyForDocumentWorks;
		private final DocumentRefreshStrategy refreshStrategyForDocumentWorks;

		private final DocumentWorkCallKind kind;
		private final CompletableFuture<?> executionFuture;

		private DocumentWorkCallListContext(String indexName, String tenantId,
				DocumentCommitStrategy commitStrategyForDocumentWorks,
				DocumentRefreshStrategy refreshStrategyForDocumentWorks,
				DocumentWorkCallKind kind, CompletableFuture<?> executionFuture) {
			this.indexName = indexName;
			this.tenantId = tenantId;
			this.commitStrategyForDocumentWorks = commitStrategyForDocumentWorks;
			this.refreshStrategyForDocumentWorks = refreshStrategyForDocumentWorks;
			this.kind = kind;
			this.executionFuture = executionFuture;
		}

		public DocumentWorkCallListContext createFollowingWorks() {
			return newContext( DocumentWorkCallKind.CREATE );
		}

		public DocumentWorkCallListContext discardFollowingWorks() {
			return newContext( DocumentWorkCallKind.DISCARD );
		}

		public DocumentWorkCallListContext executeFollowingWorks() {
			return newContext( DocumentWorkCallKind.EXECUTE );
		}

		public DocumentWorkCallListContext executeFollowingWorks(CompletableFuture<?> executionFuture) {
			return newContext( DocumentWorkCallKind.EXECUTE, executionFuture );
		}

		public DocumentWorkCallListContext createAndExecuteFollowingWorks() {
			return newContext( DocumentWorkCallKind.CREATE_AND_EXECUTE );
		}

		public DocumentWorkCallListContext createAndExecuteFollowingWorks(CompletableFuture<?> executionFuture) {
			return newContext( DocumentWorkCallKind.CREATE_AND_EXECUTE, executionFuture );
		}

		public DocumentWorkCallListContext createAndExecuteFollowingWorksOutOfOrder() {
			return newContext( DocumentWorkCallKind.CREATE_AND_EXECUTE_OUT_OF_ORDER );
		}

		public DocumentWorkCallListContext createAndDiscardFollowingWorks() {
			return newContext( DocumentWorkCallKind.CREATE_AND_DISCARD );
		}

		private DocumentWorkCallListContext newContext(DocumentWorkCallKind kind) {
			return newContext( kind, CompletableFuture.completedFuture( null ) );
		}

		private DocumentWorkCallListContext newContext(DocumentWorkCallKind kind, CompletableFuture<?> executionFuture) {
			return new DocumentWorkCallListContext( indexName, tenantId,
					commitStrategyForDocumentWorks, refreshStrategyForDocumentWorks,
					kind, executionFuture );
		}

		public DocumentWorkCallListContext add(Consumer<StubDocumentWork.Builder> contributor) {
			return documentWork( indexingWorkExpectations.addWorkType, contributor );
		}

		public DocumentWorkCallListContext add(String id, Consumer<StubDocumentNode.Builder> documentContributor) {
			return documentWork( indexingWorkExpectations.addWorkType, id, documentContributor );
		}

		public DocumentWorkCallListContext addOrUpdate(Consumer<StubDocumentWork.Builder> contributor) {
			return documentWork( StubDocumentWork.Type.ADD_OR_UPDATE, contributor );
		}

		public DocumentWorkCallListContext addOrUpdate(String id, Consumer<StubDocumentNode.Builder> documentContributor) {
			return documentWork( StubDocumentWork.Type.ADD_OR_UPDATE, id, documentContributor );
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
			builder.tenantIdentifier( tenantId );
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
			StubTreeNodeDiffer<StubDocumentNode> documentDiffer =
					documentDiffers.getOrDefault( indexName, StubDocumentWorkAssert.DEFAULT_DOCUMENT_DIFFER );
			switch ( kind ) {
				case CREATE:
					expect( new DocumentWorkCreateCall( indexName, work, documentDiffer ) );
					break;
				case DISCARD:
					expect( new DocumentWorkDiscardCall( indexName, work, documentDiffer ) );
					break;
				case EXECUTE:
					expect( new DocumentWorkExecuteCall( indexName, work, documentDiffer, executionFuture ) );
					break;
				case CREATE_AND_DISCARD:
					expect( new DocumentWorkCreateCall( indexName, work, documentDiffer ) );
					expect( new DocumentWorkDiscardCall( indexName, work, documentDiffer ) );
					break;
				case CREATE_AND_EXECUTE:
					expect( new DocumentWorkCreateCall( indexName, work, documentDiffer ) );
					expect( new DocumentWorkExecuteCall( indexName, work, documentDiffer, executionFuture ) );
					break;
				case CREATE_AND_EXECUTE_OUT_OF_ORDER:
					expectOutOfOrder( new DocumentWorkCreateCall( indexName, work, documentDiffer ) );
					expectOutOfOrder( new DocumentWorkExecuteCall( indexName, work, documentDiffer, executionFuture ) );
					break;
			}
			return this;
		}

		private void expect(DocumentWorkCreateCall call) {
			backendBehavior().getDocumentWorkCreateCalls( call.documentKey() ).expectInOrder( call );
		}

		private void expectOutOfOrder(DocumentWorkCreateCall call) {
			backendBehavior().getDocumentWorkCreateCalls( call.documentKey() ).expectOutOfOrder( call );
		}

		private void expect(DocumentWorkDiscardCall call) {
			backendBehavior().getDocumentWorkDiscardCalls( call.documentKey() ).expectInOrder( call );
		}

		private void expect(DocumentWorkExecuteCall call) {
			backendBehavior().getDocumentWorkExecuteCalls( call.documentKey() ).expectInOrder( call );
		}

		private void expectOutOfOrder(DocumentWorkExecuteCall call) {
			backendBehavior().getDocumentWorkExecuteCalls( call.documentKey() ).expectOutOfOrder( call );
		}
	}

	public static class IndexScaleWorkCallListContext {
		private final String indexName;
		private final Set<String> tenantIdentifiers;
		private final Consumer<IndexScaleWorkCall> expectationConsumer;

		private IndexScaleWorkCallListContext(String indexName,
				Set<String> tenantIdentifiers,
				Consumer<IndexScaleWorkCall> expectationConsumer) {
			this.indexName = indexName;
			this.tenantIdentifiers = tenantIdentifiers;
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
					.tenantIdentifiers( tenantIdentifiers )
					.routingKeys( routingKeys )
					.build();
			expectationConsumer.accept( new IndexScaleWorkCall( indexName, work, future ) );
			return this;
		}
	}

}
