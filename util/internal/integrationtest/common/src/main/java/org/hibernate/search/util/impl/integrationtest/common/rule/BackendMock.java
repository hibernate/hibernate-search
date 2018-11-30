/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.rule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.hibernate.search.engine.backend.document.converter.runtime.FromIndexFieldValueConvertContext;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.SearchResult;
import org.hibernate.search.engine.search.query.spi.ProjectionHitMapper;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.StubBackendBehavior;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.StubDocumentNode;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.StubIndexSchemaNode;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.StubIndexWork;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.StubSearchWork;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl.StubSearchProjection;

import org.junit.Assert;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class BackendMock implements TestRule {

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
		CallQueue<PushSchemaCall> callQueue = behaviorMock.getPushSchemaCalls( indexName );
		StubIndexSchemaNode.Builder builder = StubIndexSchemaNode.schema();
		contributor.accept( builder );
		callQueue.expectOutOfOrder( new PushSchemaCall( indexName, builder.build() ) );
		return this;
	}

	public BackendMock expectAnySchema(String indexName) {
		CallQueue<PushSchemaCall> callQueue = behaviorMock.getPushSchemaCalls( indexName );
		callQueue.expectOutOfOrder( new PushSchemaCall( indexName, null ) );
		return this;
	}

	public WorkCallListContext expectWorks(String indexName) {
		CallQueue<IndexWorkCall> callQueue = behaviorMock.getIndexWorkCalls( indexName );
		return new WorkCallListContext( indexName, callQueue::expectInOrder );
	}

	public WorkCallListContext expectWorksAnyOrder(String indexName) {
		CallQueue<IndexWorkCall> callQueue = behaviorMock.getIndexWorkCalls( indexName );
		return new WorkCallListContext( indexName, callQueue::expectOutOfOrder );
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

	public class WorkCallListContext {
		private final String indexName;
		private final Consumer<IndexWorkCall> expectationConsumer;
		private final List<StubIndexWork> works = new ArrayList<>();

		private WorkCallListContext(String indexName, Consumer<IndexWorkCall> expectationConsumer) {
			this.indexName = indexName;
			this.expectationConsumer = expectationConsumer;
		}

		public WorkCallListContext add(Consumer<StubIndexWork.Builder> contributor) {
			return work( StubIndexWork.Type.ADD, contributor );
		}

		public WorkCallListContext add(String id, Consumer<StubDocumentNode.Builder> documentContributor) {
			return work( StubIndexWork.Type.ADD, id, documentContributor );
		}

		public WorkCallListContext update(Consumer<StubIndexWork.Builder> contributor) {
			return work( StubIndexWork.Type.UPDATE, contributor );
		}

		public WorkCallListContext update(String id, Consumer<StubDocumentNode.Builder> documentContributor) {
			return work( StubIndexWork.Type.UPDATE, id, documentContributor );
		}

		public WorkCallListContext delete(String id) {
			return work( StubIndexWork.Type.DELETE, b -> b.identifier( id ) );
		}

		public WorkCallListContext delete(Consumer<StubIndexWork.Builder> contributor) {
			return work( StubIndexWork.Type.DELETE, contributor );
		}

		public WorkCallListContext optimize() {
			return work( StubIndexWork.builder( StubIndexWork.Type.OPTIMIZE ).build() );
		}

		public WorkCallListContext purge(String tenantIdentifier) {
			return work( StubIndexWork.builder( StubIndexWork.Type.PURGE ).tenantIdentifier( tenantIdentifier ).build() );
		}

		public WorkCallListContext flush() {
			return work( StubIndexWork.builder( StubIndexWork.Type.FLUSH ).build() );
		}

		WorkCallListContext work(StubIndexWork.Type type, Consumer<StubIndexWork.Builder> contributor) {
			StubIndexWork.Builder builder = StubIndexWork.builder( type );
			contributor.accept( builder );
			return work( builder.build() );
		}

		WorkCallListContext work(StubIndexWork.Type type, String id,
				Consumer<StubDocumentNode.Builder> documentContributor) {
			return work( type, b -> {
				b.identifier( id );
				StubDocumentNode.Builder documentBuilder = StubDocumentNode.document();
				documentContributor.accept( documentBuilder );
				b.document( documentBuilder.build() );
			} );
		}

		public WorkCallListContext work(StubIndexWork work) {
			works.add( work );
			return this;
		}

		public BackendMock preparedThenExecuted() {
			// First expect all works to be prepared, then expect all works to be executed
			works.stream()
					.map( work -> new IndexWorkCall( indexName, IndexWorkCall.WorkPhase.PREPARE, work ) )
					.forEach( expectationConsumer );
			works.stream()
					.map( work -> new IndexWorkCall( indexName, IndexWorkCall.WorkPhase.EXECUTE, work ) )
					.forEach( expectationConsumer );
			return BackendMock.this;
		}

		public BackendMock executed() {
			works.stream()
					.map( work -> new IndexWorkCall( indexName, IndexWorkCall.WorkPhase.EXECUTE, work ) )
					.forEach( expectationConsumer );
			return BackendMock.this;
		}

		public BackendMock prepared() {
			works.stream()
					.map( work -> new IndexWorkCall( indexName, IndexWorkCall.WorkPhase.PREPARE, work ) )
					.forEach( expectationConsumer );
			return BackendMock.this;
		}
	}

	private class VerifyingStubBackendBehavior extends StubBackendBehavior {

		private final Map<IndexFieldKey, IndexFieldAddBehavior> indexFieldAddBehaviors = new HashMap<>();

		private final Map<String, CallQueue<PushSchemaCall>> pushSchemaCalls = new HashMap<>();

		private final Map<String, CallQueue<IndexWorkCall>> indexWorkCalls = new HashMap<>();

		private final CallQueue<SearchWorkCall<?>> searchCalls = new CallQueue<>();

		void setIndexFieldAddBehavior(String indexName, String absoluteFieldPath, IndexFieldAddBehavior behavior) {
			indexFieldAddBehaviors.put( new IndexFieldKey( indexName, absoluteFieldPath ), behavior );
		}

		CallQueue<PushSchemaCall> getPushSchemaCalls(String indexName) {
			return pushSchemaCalls.computeIfAbsent( indexName, ignored -> new CallQueue<>() );
		}

		CallQueue<IndexWorkCall> getIndexWorkCalls(String indexName) {
			return indexWorkCalls.computeIfAbsent( indexName, ignored -> new CallQueue<>() );
		}

		CallQueue<SearchWorkCall<?>> getSearchWorkCalls() {
			return searchCalls;
		}

		void resetExpectations() {
			indexFieldAddBehaviors.clear();
			pushSchemaCalls.clear();
			indexWorkCalls.clear();
			searchCalls.reset();
		}

		void verifyExpectationsMet() {
			pushSchemaCalls.values().forEach( CallQueue::verifyExpectationsMet );
			indexWorkCalls.values().forEach( CallQueue::verifyExpectationsMet );
			searchCalls.verifyExpectationsMet();
		}

		@Override
		public void onAddField(String indexName, String absoluteFieldPath) {
			IndexFieldAddBehavior behavior = indexFieldAddBehaviors.get( new IndexFieldKey( indexName, absoluteFieldPath ) );
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
		public void prepareWorks(String indexName, List<StubIndexWork> works) {
			CallQueue<IndexWorkCall> callQueue = getIndexWorkCalls( indexName );
			works.stream()
					.map( work -> new IndexWorkCall( indexName, IndexWorkCall.WorkPhase.PREPARE, work ) )
					.forEach( call -> callQueue.verify( call, IndexWorkCall::verify ) );
		}

		@Override
		public CompletableFuture<?> executeWorks(String indexName, List<StubIndexWork> works) {
			CallQueue<IndexWorkCall> callQueue = getIndexWorkCalls( indexName );
			return works.stream()
					.map( work -> new IndexWorkCall( indexName, IndexWorkCall.WorkPhase.EXECUTE, work ) )
					.<CompletableFuture<?>>map( call -> callQueue.verify( call, IndexWorkCall::verify ) )
					.reduce( (first, second) -> second )
					.orElseGet( () -> CompletableFuture.completedFuture( null ) );
		}

		@Override
		public CompletableFuture<?> prepareAndExecuteWork(String indexName, StubIndexWork work) {
			CallQueue<IndexWorkCall> callQueue = getIndexWorkCalls( indexName );
			callQueue.verify(
					new IndexWorkCall( indexName, IndexWorkCall.WorkPhase.PREPARE, work ),
					IndexWorkCall::verify
			);
			callQueue.verify(
					new IndexWorkCall( indexName, IndexWorkCall.WorkPhase.EXECUTE, work ),
					IndexWorkCall::verify
			);

			return CompletableFuture.completedFuture( null );
		}

		@Override
		public <T> SearchResult<T> executeSearchWork(List<String> indexNames, StubSearchWork work,
				FromIndexFieldValueConvertContext convertContext,
				ProjectionHitMapper<?, ?> projectionHitMapper, StubSearchProjection<T> rootProjection) {
			return searchCalls.verify(
					new SearchWorkCall<>( indexNames, work, convertContext, projectionHitMapper, rootProjection ),
					SearchWorkCall::<T>verify );
		}

		@Override
		public CompletableFuture<?> executeBulkWork(String indexName, StubIndexWork work) {
			if ( work.getDocument() != null ) {
				Assert.fail( "A bulk work is supposed not to have a document. Actual work: " + work );
			}

			CallQueue<IndexWorkCall> callQueue = getIndexWorkCalls( indexName );
			callQueue.verify(
					new IndexWorkCall( indexName, IndexWorkCall.WorkPhase.EXECUTE, work ),
					IndexWorkCall::verify
			);

			return CompletableFuture.completedFuture( null );
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
