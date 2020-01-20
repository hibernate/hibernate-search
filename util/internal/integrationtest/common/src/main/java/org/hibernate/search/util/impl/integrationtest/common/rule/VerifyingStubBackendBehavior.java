/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.rule;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

import org.hibernate.search.engine.search.loading.context.spi.LoadingContext;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.engine.search.query.spi.SimpleSearchResult;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.StubBackendBehavior;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.StubIndexSchemaNode;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.StubDocumentWork;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.StubIndexScopeWork;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.StubSearchWork;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl.StubSearchProjection;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl.StubSearchProjectionContext;

import org.junit.Assert;

class VerifyingStubBackendBehavior extends StubBackendBehavior {

	private final Map<IndexFieldKey, CallBehavior<Void>> indexFieldAddBehaviors = new HashMap<>();

	private final Map<String, CallQueue<PushSchemaCall>> pushSchemaCalls = new HashMap<>();

	private final Map<String, CallQueue<DocumentWorkCall>> documentWorkCalls = new HashMap<>();

	private final CallQueue<IndexScopeWorkCall> indexScopeWorkCalls = new CallQueue<>();

	private final CallQueue<SearchWorkCall<?>> searchCalls = new CallQueue<>();

	private final CallQueue<CountWorkCall> countCalls = new CallQueue<>();

	private boolean lenient = false;

	void setLenient(boolean lenient) {
		this.lenient = lenient;
	}

	void setIndexFieldAddBehavior(String indexName, String absoluteFieldPath, CallBehavior<Void> behavior) {
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
		CallBehavior<Void> behavior = indexFieldAddBehaviors.get( new IndexFieldKey( indexName, absoluteFieldPath ) );
		if ( behavior != null ) {
			behavior.execute();
		}
	}

	@Override
	public void pushSchema(String indexName, StubIndexSchemaNode rootSchemaNode) {
		getPushSchemaCalls( indexName )
				.verify(
						new PushSchemaCall( indexName, rootSchemaNode ),
						PushSchemaCall::verify,
						noExpectationsBehavior( () -> null )
				);
	}

	@Override
	public void processDocumentWork(String indexName, StubDocumentWork work) {
		CallQueue<DocumentWorkCall> callQueue = getDocumentWorkCalls( indexName );
		callQueue.verify(
				new DocumentWorkCall( indexName, DocumentWorkCall.WorkPhase.PROCESS, work ),
				DocumentWorkCall::verify,
				noExpectationsBehavior( () -> CompletableFuture.completedFuture( null ) )
		);
	}

	@Override
	public void discardDocumentWork(String indexName, StubDocumentWork work) {
		CallQueue<DocumentWorkCall> callQueue = getDocumentWorkCalls( indexName );
		callQueue.verify(
				new DocumentWorkCall( indexName, DocumentWorkCall.WorkPhase.DISCARD, work ),
				DocumentWorkCall::verify,
				noExpectationsBehavior( () -> CompletableFuture.completedFuture( null ) )
		);
	}

	@Override
	public CompletableFuture<?> executeDocumentWork(String indexName, StubDocumentWork work) {
		CallQueue<DocumentWorkCall> callQueue = getDocumentWorkCalls( indexName );
		return callQueue.verify(
				new DocumentWorkCall( indexName, DocumentWorkCall.WorkPhase.EXECUTE, work ),
				DocumentWorkCall::verify,
				noExpectationsBehavior( () -> CompletableFuture.completedFuture( null ) )
		);
	}

	@Override
	public CompletableFuture<?> processAndExecuteDocumentWork(String indexName, StubDocumentWork work) {
		CallQueue<DocumentWorkCall> callQueue = getDocumentWorkCalls( indexName );
		callQueue.verify(
				new DocumentWorkCall( indexName, DocumentWorkCall.WorkPhase.PROCESS, work ),
				DocumentWorkCall::verify,
				noExpectationsBehavior( () -> CompletableFuture.completedFuture( null ) )
		);
		return callQueue.verify(
				new DocumentWorkCall( indexName, DocumentWorkCall.WorkPhase.EXECUTE, work ),
				DocumentWorkCall::verify,
				noExpectationsBehavior( () -> CompletableFuture.completedFuture( null ) )
		);
	}

	@Override
	public <T> SearchResult<T> executeSearchWork(Set<String> indexNames, StubSearchWork work,
			StubSearchProjectionContext projectionContext,
			LoadingContext<?, ?> loadingContext, StubSearchProjection<T> rootProjection) {
		return searchCalls.verify(
				new SearchWorkCall<>( indexNames, work, projectionContext, loadingContext, rootProjection ),
				(call1, call2) -> call1.verify( call2 ),
				noExpectationsBehavior( () -> new SimpleSearchResult<>(
						0L, Collections.emptyList(), Collections.emptyMap(), Duration.ZERO, false
				) )
		);
	}

	@Override
	public CompletableFuture<?> executeIndexScopeWork(Set<String> indexNames, StubIndexScopeWork work) {
		return indexScopeWorkCalls.verify(
				new IndexScopeWorkCall( indexNames, work ),
				IndexScopeWorkCall::verify,
				noExpectationsBehavior( () -> CompletableFuture.completedFuture( null ) )
		);
	}

	@Override
	public long executeCountWork(Set<String> indexNames) {
		return countCalls.verify(
				new CountWorkCall( indexNames, null ),
				CountWorkCall::verify,
				noExpectationsBehavior( () -> 0L )
		);
	}

	private <C, T> Function<C, T> noExpectationsBehavior(Supplier<T> lenientResultSupplier) {
		if ( lenient ) {
			return ignored -> lenientResultSupplier.get();
		}
		else {
			return strictNoExpectationsBehavior();
		}
	}

	private static <C, T> Function<C, T> strictNoExpectationsBehavior() {
		return call -> {
			Assert.fail( "No call expected, but got: " + call );
			// Dead code, we throw an exception above
			return null;
		};
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
			if ( ! (obj instanceof IndexFieldKey ) ) {
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
