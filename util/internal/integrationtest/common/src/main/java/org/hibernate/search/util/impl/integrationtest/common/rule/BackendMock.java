/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.rule;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentFieldValueConvertContext;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContext;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.util.common.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.StubBackendBehavior;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.StubDocumentNode;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.StubIndexSchemaNode;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.StubDocumentWork;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.StubIndexScopeWork;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.StubSearchWork;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl.StubSearchProjection;

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

	public IndexScopeWorkCallListContext expectIndexScopeWorks(List<String> indexNames, String tenantId) {
		CallQueue<IndexScopeWorkCall> callQueue = behaviorMock.getIndexScopeWorkCalls();
		return new IndexScopeWorkCallListContext(
				indexNames, tenantId,
				callQueue::expectInOrder
		);
	}

	public BackendMock expectSearchReferences(List<String> indexNames, Consumer<StubSearchWork.Builder> contributor,
			StubSearchWorkBehavior<DocumentReference> behavior) {
		return expectSearch( indexNames, contributor, StubSearchWork.ResultType.REFERENCES, behavior );
	}

	public BackendMock expectSearchObjects(List<String> indexNames, Consumer<StubSearchWork.Builder> contributor,
			StubSearchWorkBehavior<DocumentReference> behavior) {
		return expectSearch( indexNames, contributor, StubSearchWork.ResultType.OBJECTS, behavior );
	}

	public BackendMock expectSearchProjections(List<String> indexNames, Consumer<StubSearchWork.Builder> contributor,
			StubSearchWorkBehavior<List<?>> behavior) {
		return expectSearch( indexNames, contributor, StubSearchWork.ResultType.PROJECTIONS, behavior );
	}

	public BackendMock expectSearchProjection(List<String> indexNames, Consumer<StubSearchWork.Builder> contributor,
			StubSearchWorkBehavior<?> behavior) {
		return expectSearch( indexNames, contributor, StubSearchWork.ResultType.PROJECTIONS, behavior );
	}

	private BackendMock expectSearch(List<String> indexNames, Consumer<StubSearchWork.Builder> contributor,
			StubSearchWork.ResultType resultType, StubSearchWorkBehavior<?> behavior) {
		CallQueue<SearchWorkCall<?>> callQueue = behaviorMock.getSearchWorkCalls();
		StubSearchWork.Builder builder = StubSearchWork.builder( resultType );
		contributor.accept( builder );
		callQueue.expectInOrder( new SearchWorkCall<>( indexNames, builder.build(), behavior ) );
		return this;
	}

	public BackendMock expectCount(List<String> indexNames, long expectedResult) {
		CallQueue<CountWorkCall> callQueue = behaviorMock.getCountWorkCalls();
		callQueue.expectInOrder( new CountWorkCall( indexNames, expectedResult ) );
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

		public BackendMock preparedThenExecuted(CompletableFuture<?> future) {
			log.debugf( "Expecting %d works to be prepared, then executed", works.size() );
			// First expect all works to be prepared, then expect all works to be executed
			works.stream()
					.map( work -> new DocumentWorkCall( indexName, DocumentWorkCall.WorkPhase.PREPARE, work ) )
					.forEach( expectationConsumer );
			works.stream()
					.map( work -> new DocumentWorkCall( indexName, DocumentWorkCall.WorkPhase.EXECUTE, work, future ) )
					.forEach( expectationConsumer );
			return BackendMock.this;
		}

		public BackendMock preparedThenExecuted() {
			return preparedThenExecuted( CompletableFuture.completedFuture( null ) );
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

		public BackendMock prepared() {
			log.debugf( "Expecting %d works to be prepared", works.size() );
			works.stream()
					.map( work -> new DocumentWorkCall( indexName, DocumentWorkCall.WorkPhase.PREPARE, work ) )
					.forEach( expectationConsumer );
			return BackendMock.this;
		}
	}

	public class IndexScopeWorkCallListContext {
		private final List<String> indexNames;
		private final String tenantIdentifier;
		private final Consumer<IndexScopeWorkCall> expectationConsumer;

		private IndexScopeWorkCallListContext(List<String> indexNames,
				String tenantIdentifier,
				Consumer<IndexScopeWorkCall> expectationConsumer) {
			this.indexNames = indexNames;
			this.tenantIdentifier = tenantIdentifier;
			this.expectationConsumer = expectationConsumer;
		}

		public IndexScopeWorkCallListContext optimize() {
			return optimize( CompletableFuture.completedFuture( null ) );
		}

		public IndexScopeWorkCallListContext optimize(CompletableFuture<?> future) {
			return work(
					StubIndexScopeWork.builder( StubIndexScopeWork.Type.OPTIMIZE ).build(),
					future
			);
		}

		public IndexScopeWorkCallListContext purge() {
			return purge( CompletableFuture.completedFuture( null ) );
		}

		public IndexScopeWorkCallListContext purge(CompletableFuture<?> future) {
			return work(
					StubIndexScopeWork.builder( StubIndexScopeWork.Type.PURGE )
							.tenantIdentifier( tenantIdentifier )
							.build(),
					future
			);
		}

		public IndexScopeWorkCallListContext flush() {
			return flush( CompletableFuture.completedFuture( null ) );
		}

		public IndexScopeWorkCallListContext flush(CompletableFuture<?> future) {
			return work(
					StubIndexScopeWork.builder( StubIndexScopeWork.Type.FLUSH ).build(),
					future
			);
		}

		private IndexScopeWorkCallListContext work(StubIndexScopeWork work, CompletableFuture<?> future) {
			expectationConsumer.accept( new IndexScopeWorkCall( indexNames, work, future ) );
			return this;
		}
	}

	private class VerifyingStubBackendBehavior extends StubBackendBehavior {

		private final Map<IndexFieldKey, CallBehavior> indexFieldAddBehaviors = new HashMap<>();

		private final Map<String, CallQueue<PushSchemaCall>> pushSchemaCalls = new HashMap<>();

		private final Map<String, CallQueue<DocumentWorkCall>> documentWorkCalls = new HashMap<>();

		private final CallQueue<IndexScopeWorkCall> indexScopeWorkCalls = new CallQueue<>();

		private final CallQueue<SearchWorkCall<?>> searchCalls = new CallQueue<>();

		private final CallQueue<CountWorkCall> countCalls = new CallQueue<>();

		void setIndexFieldAddBehavior(String indexName, String absoluteFieldPath, CallBehavior behavior) {
			indexFieldAddBehaviors.put( new IndexFieldKey( indexName, absoluteFieldPath ), behavior );
		}

		CallQueue<PushSchemaCall> getPushSchemaCalls(String indexName) {
			return pushSchemaCalls.computeIfAbsent( indexName, ignored -> new CallQueue<>() );
		}

		CallQueue<DocumentWorkCall> getDocumentWorkCalls(String indexName) {
			return documentWorkCalls.computeIfAbsent( indexName, ignored -> new CallQueue<>() );
		}

		CallQueue<IndexScopeWorkCall> getIndexScopeWorkCalls() {
			return indexScopeWorkCalls;
		}

		CallQueue<SearchWorkCall<?>> getSearchWorkCalls() {
			return searchCalls;
		}

		CallQueue<CountWorkCall> getCountWorkCalls() {
			return countCalls;
		}

		void resetExpectations() {
			indexFieldAddBehaviors.clear();
			pushSchemaCalls.clear();
			documentWorkCalls.clear();
			searchCalls.reset();
		}

		void verifyExpectationsMet() {
			pushSchemaCalls.values().forEach( CallQueue::verifyExpectationsMet );
			documentWorkCalls.values().forEach( CallQueue::verifyExpectationsMet );
			searchCalls.verifyExpectationsMet();
		}

		@Override
		public void onAddField(String indexName, String absoluteFieldPath) {
			CallBehavior behavior = indexFieldAddBehaviors.get( new IndexFieldKey( indexName, absoluteFieldPath ) );
			if ( behavior != null ) {
				behavior.execute();
			}
		}

		@Override
		public void pushSchema(String indexName, StubIndexSchemaNode rootSchemaNode) {
			getPushSchemaCalls( indexName )
					.verify( new PushSchemaCall( indexName, rootSchemaNode ), PushSchemaCall::verify );
		}

		@Override
		public void prepareDocumentWorks(String indexName, List<StubDocumentWork> works) {
			CallQueue<DocumentWorkCall> callQueue = getDocumentWorkCalls( indexName );
			works.stream()
					.map( work -> new DocumentWorkCall( indexName, DocumentWorkCall.WorkPhase.PREPARE, work ) )
					.forEach( call -> callQueue.verify( call, DocumentWorkCall::verify ) );
		}

		@Override
		public CompletableFuture<?> executeDocumentWorks(String indexName, List<StubDocumentWork> works) {
			CallQueue<DocumentWorkCall> callQueue = getDocumentWorkCalls( indexName );
			return works.stream()
					.map( work -> new DocumentWorkCall( indexName, DocumentWorkCall.WorkPhase.EXECUTE, work ) )
					.<CompletableFuture<?>>map( call -> callQueue.verify( call, DocumentWorkCall::verify ) )
					.reduce( (first, second) -> second )
					.orElseGet( () -> CompletableFuture.completedFuture( null ) );
		}

		@Override
		public CompletableFuture<?> prepareAndExecuteDocumentWork(String indexName, StubDocumentWork work) {
			CallQueue<DocumentWorkCall> callQueue = getDocumentWorkCalls( indexName );
			callQueue.verify(
					new DocumentWorkCall( indexName, DocumentWorkCall.WorkPhase.PREPARE, work ),
					DocumentWorkCall::verify
			);
			return callQueue.verify(
					new DocumentWorkCall( indexName, DocumentWorkCall.WorkPhase.EXECUTE, work ),
					DocumentWorkCall::verify
			);
		}

		@Override
		public <T> SearchResult<T> executeSearchWork(List<String> indexNames, StubSearchWork work,
				FromDocumentFieldValueConvertContext convertContext,
				LoadingContext<?, ?> loadingContext, StubSearchProjection<T> rootProjection) {
			return searchCalls.verify(
					new SearchWorkCall<>( indexNames, work, convertContext, loadingContext, rootProjection ),
					(call1, call2) -> call1.verify( call2 )
			);
		}

		@Override
		public CompletableFuture<?> executeIndexScopeWork(List<String> indexNames, StubIndexScopeWork work) {
			return indexScopeWorkCalls.verify(
					new IndexScopeWorkCall( indexNames, work ),
					IndexScopeWorkCall::verify
			);
		}

		@Override
		public long executeCountWork(List<String> indexNames) {
			return countCalls.verify( new CountWorkCall( indexNames, null ), CountWorkCall::verify );
		}
	}

	private static class IndexFieldKey {
		final String indexName;
		final String absoluteFieldPath;

		private IndexFieldKey(String indexName, String absoluteFieldPath) {
			this.indexName = indexName;
			this.absoluteFieldPath = absoluteFieldPath;
		}

		@Override
		public boolean equals(Object obj) {
			if ( ! (obj instanceof IndexFieldKey) ) {
				return false;
			}
			IndexFieldKey other = (IndexFieldKey) obj;
			return Objects.equals( indexName, other.indexName )
					&& Objects.equals( absoluteFieldPath, other.absoluteFieldPath );
		}

		@Override
		public int hashCode() {
			return Objects.hash( indexName, absoluteFieldPath );
		}
	}

}
