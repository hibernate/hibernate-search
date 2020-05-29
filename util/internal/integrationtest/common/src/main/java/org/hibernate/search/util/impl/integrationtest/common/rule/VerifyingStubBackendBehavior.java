/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.rule;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

import org.hibernate.search.engine.backend.spi.BackendBuildContext;
import org.hibernate.search.engine.reporting.spi.ContextualFailureCollector;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContext;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.engine.search.query.spi.SimpleSearchResult;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.StubBackendBehavior;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.StubIndexSchemaNode;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.StubDocumentWork;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.StubIndexScaleWork;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.StubSchemaManagementWork;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.StubSearchWork;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl.StubSearchProjection;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl.StubSearchProjectionContext;

import org.junit.Assert;

class VerifyingStubBackendBehavior extends StubBackendBehavior {

	private final Map<IndexFieldKey, CallBehavior<Void>> indexFieldAddBehaviors = new HashMap<>();

	private final List<ParameterizedCallBehavior<BackendBuildContext, Void>> createBackendBehaviors = new ArrayList<>();

	private final List<CallBehavior<Void>> stopBackendBehaviors = new ArrayList<>();

	private final Map<String, CallQueue<SchemaDefinitionCall>> schemaDefinitionCalls = new HashMap<>();

	private final Map<String, CallQueue<SchemaManagementWorkCall>> schemaManagementWorkCall = new HashMap<>();

	private final Map<String, CallQueue<DocumentWorkCall>> documentWorkCalls = new HashMap<>();

	private final CallQueue<IndexScaleWorkCall> indexScaleWorkCalls = new CallQueue<>();

	private final CallQueue<SearchWorkCall<?>> searchCalls = new CallQueue<>();

	private final CallQueue<CountWorkCall> countCalls = new CallQueue<>();

	private boolean lenient = false;

	void setLenient(boolean lenient) {
		this.lenient = lenient;
	}

	public void addCreateBackendBehavior(ParameterizedCallBehavior<BackendBuildContext, Void> createBackendBehavior) {
		this.createBackendBehaviors.add( createBackendBehavior );
	}

	public void addStopBackendBehavior(CallBehavior<Void> stopBackendBehavior) {
		this.stopBackendBehaviors.add( stopBackendBehavior );
	}

	void setIndexFieldAddBehavior(String indexName, String absoluteFieldPath, CallBehavior<Void> behavior) {
		indexFieldAddBehaviors.put( new IndexFieldKey( indexName, absoluteFieldPath ), behavior );
	}

	CallQueue<SchemaDefinitionCall> getSchemaDefinitionCalls(String indexName) {
		return schemaDefinitionCalls.computeIfAbsent( indexName, ignored -> new CallQueue<>() );
	}

	CallQueue<SchemaManagementWorkCall> getSchemaManagementWorkCalls(String indexName) {
		return schemaManagementWorkCall.computeIfAbsent( indexName, ignored -> new CallQueue<>() );
	}

	CallQueue<DocumentWorkCall> getDocumentWorkCalls(String indexName) {
		return documentWorkCalls.computeIfAbsent( indexName, ignored -> new CallQueue<>() );
	}

	CallQueue<IndexScaleWorkCall> getIndexScaleWorkCalls() {
		return indexScaleWorkCalls;
	}

	CallQueue<SearchWorkCall<?>> getSearchWorkCalls() {
		return searchCalls;
	}

	CallQueue<CountWorkCall> getCountWorkCalls() {
		return countCalls;
	}

	void resetExpectations() {
		indexFieldAddBehaviors.clear();
		createBackendBehaviors.clear();
		stopBackendBehaviors.clear();
		schemaDefinitionCalls.clear();
		schemaManagementWorkCall.clear();
		documentWorkCalls.clear();
		indexScaleWorkCalls.reset();
		searchCalls.reset();
		countCalls.reset();
	}

	void verifyExpectationsMet() {
		// We don't check anything for the various behaviors (createBackendBehaviors, ...): they are ignored if they are not executed.
		schemaDefinitionCalls.values().forEach( CallQueue::verifyExpectationsMet );
		schemaManagementWorkCall.values().forEach( CallQueue::verifyExpectationsMet );
		documentWorkCalls.values().forEach( CallQueue::verifyExpectationsMet );
		indexScaleWorkCalls.verifyExpectationsMet();
		searchCalls.verifyExpectationsMet();
		countCalls.verifyExpectationsMet();
	}

	@Override
	public void onCreateBackend(BackendBuildContext context) {
		for ( ParameterizedCallBehavior<BackendBuildContext, Void> behavior : createBackendBehaviors ) {
			behavior.execute( context );
		}
	}

	@Override
	public void onStopBackend() {
		for ( CallBehavior<Void> behavior : stopBackendBehaviors ) {
			behavior.execute();
		}
	}

	@Override
	public void onAddField(String indexName, String absoluteFieldPath) {
		CallBehavior<Void> behavior = indexFieldAddBehaviors.get( new IndexFieldKey( indexName, absoluteFieldPath ) );
		if ( behavior != null ) {
			behavior.execute();
		}
	}

	@Override
	public void defineSchema(String indexName, StubIndexSchemaNode rootSchemaNode) {
		getSchemaDefinitionCalls( indexName )
				.verify(
						new SchemaDefinitionCall( indexName, rootSchemaNode ),
						SchemaDefinitionCall::verify,
						noExpectationsBehavior( () -> null )
				);
	}

	@Override
	public CompletableFuture<?> executeSchemaManagementWork(String indexName, StubSchemaManagementWork work,
			ContextualFailureCollector failureCollector) {
		return getSchemaManagementWorkCalls( indexName )
				.verify(
						new SchemaManagementWorkCall( indexName, work, failureCollector ),
						SchemaManagementWorkCall::verify,
						noExpectationsBehavior( () -> CompletableFuture.completedFuture( null ) )
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
	public CompletableFuture<?> executeIndexScaleWork(String indexName, StubIndexScaleWork work) {
		return indexScaleWorkCalls.verify(
				new IndexScaleWorkCall( indexName, work ),
				IndexScaleWorkCall::verify,
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
