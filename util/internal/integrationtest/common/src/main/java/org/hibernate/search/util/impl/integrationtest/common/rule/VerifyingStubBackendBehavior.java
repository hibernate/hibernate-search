/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.rule;

import static org.junit.Assert.fail;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

import org.hibernate.search.engine.backend.spi.BackendBuildContext;
import org.hibernate.search.engine.common.timing.spi.TimingSource;
import org.hibernate.search.engine.reporting.spi.ContextualFailureCollector;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContext;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.engine.search.query.SearchScroll;
import org.hibernate.search.engine.search.query.SearchScrollResult;
import org.hibernate.search.engine.search.query.spi.SimpleSearchResult;
import org.hibernate.search.engine.search.query.spi.SimpleSearchResultTotal;
import org.hibernate.search.engine.search.query.spi.SimpleSearchScrollResult;
import org.hibernate.search.engine.common.timing.spi.Deadline;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.StubBackendBehavior;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.StubIndexSchemaNode;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.StubDocumentWork;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.StubIndexScaleWork;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.StubSchemaManagementWork;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.StubSearchScroll;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.StubSearchWork;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl.StubSearchProjection;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl.StubSearchProjectionContext;

class VerifyingStubBackendBehavior extends StubBackendBehavior {

	private final Map<IndexFieldKey, CallBehavior<Void>> indexFieldAddBehaviors = new HashMap<>();

	private final List<ParameterizedCallBehavior<BackendBuildContext, Void>> createBackendBehaviors = new ArrayList<>();

	private final List<CallBehavior<Void>> stopBackendBehaviors = new ArrayList<>();

	private final Map<String, CallQueue<IndexScaleWorkCall>> indexScaleWorkCalls = new HashMap<>();

	private final Map<String, CallQueue<SchemaDefinitionCall>> schemaDefinitionCalls = new HashMap<>();

	private final Map<String, CallQueue<SchemaManagementWorkCall>> schemaManagementWorkCall = new HashMap<>();

	private final Map<DocumentKey, CallQueue<DocumentWorkCreateCall>> documentWorkCreateCalls = new LinkedHashMap<>();
	private final Map<DocumentKey, CallQueue<DocumentWorkDiscardCall>> documentWorkDiscardCalls = new LinkedHashMap<>();
	private final Map<DocumentKey, CallQueue<DocumentWorkExecuteCall>> documentWorkExecuteCalls = new LinkedHashMap<>();

	private final CallQueue<SearchWorkCall<?>> searchCalls = new CallQueue<>();

	private final CallQueue<CountWorkCall> countCalls = new CallQueue<>();

	private final CallQueue<ScrollWorkCall<?>> scrollCalls = new CallQueue<>();

	private final CallQueue<CloseScrollWorkCall> closeScrollCalls = new CallQueue<>();

	private final CallQueue<NextScrollWorkCall<?>> nextScrollCalls = new CallQueue<>();

	private boolean lenient = false;

	private boolean ignoreSchema = false;

	void lenient(boolean lenient) {
		this.lenient = lenient;
	}

	void ignoreSchema(boolean ignoreSchema) {
		this.ignoreSchema = ignoreSchema;
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

	CallQueue<DocumentWorkCreateCall> getDocumentWorkCreateCalls(DocumentKey documentKey) {
		return documentWorkCreateCalls.computeIfAbsent( documentKey, ignored -> new CallQueue<>() );
	}

	CallQueue<DocumentWorkDiscardCall> getDocumentWorkDiscardCalls(DocumentKey documentKey) {
		return documentWorkDiscardCalls.computeIfAbsent( documentKey, ignored -> new CallQueue<>() );
	}

	CallQueue<DocumentWorkExecuteCall> getDocumentWorkExecuteCalls(DocumentKey documentKey) {
		return documentWorkExecuteCalls.computeIfAbsent( documentKey, ignored -> new CallQueue<>() );
	}

	CallQueue<IndexScaleWorkCall> getIndexScaleWorkCalls(String indexName) {
		return indexScaleWorkCalls.computeIfAbsent( indexName, ignored -> new CallQueue<>() );
	}

	CallQueue<SearchWorkCall<?>> getSearchWorkCalls() {
		return searchCalls;
	}

	CallQueue<CountWorkCall> getCountWorkCalls() {
		return countCalls;
	}

	CallQueue<ScrollWorkCall<?>> getScrollCalls() {
		return scrollCalls;
	}

	CallQueue<CloseScrollWorkCall> getCloseScrollCalls() {
		return closeScrollCalls;
	}

	CallQueue<NextScrollWorkCall<?>> getNextScrollCalls() {
		return nextScrollCalls;
	}

	void resetExpectations() {
		indexFieldAddBehaviors.clear();
		createBackendBehaviors.clear();
		stopBackendBehaviors.clear();
		schemaDefinitionCalls.clear();
		indexScaleWorkCalls.clear();
		schemaManagementWorkCall.clear();
		documentWorkCreateCalls.clear();
		documentWorkDiscardCalls.clear();
		documentWorkExecuteCalls.clear();
		searchCalls.reset();
		countCalls.reset();
		scrollCalls.reset();
		closeScrollCalls.reset();
		nextScrollCalls.reset();
		scrollCalls.reset();
		closeScrollCalls.reset();
		nextScrollCalls.reset();
	}

	void verifyExpectationsMet() {
		// We don't check anything for the various behaviors (createBackendBehaviors, ...): they are ignored if they are not executed.
		schemaDefinitionCalls.values().forEach( CallQueue::verifyExpectationsMet );
		indexScaleWorkCalls.values().forEach( CallQueue::verifyExpectationsMet );
		schemaManagementWorkCall.values().forEach( CallQueue::verifyExpectationsMet );
		documentWorkCreateCalls.values().forEach( CallQueue::verifyExpectationsMet );
		documentWorkDiscardCalls.values().forEach( CallQueue::verifyExpectationsMet );
		documentWorkExecuteCalls.values().forEach( CallQueue::verifyExpectationsMet );
		searchCalls.verifyExpectationsMet();
		countCalls.verifyExpectationsMet();
		scrollCalls.verifyExpectationsMet();
		closeScrollCalls.verifyExpectationsMet();
		nextScrollCalls.verifyExpectationsMet();
		scrollCalls.verifyExpectationsMet();
		closeScrollCalls.verifyExpectationsMet();
		nextScrollCalls.verifyExpectationsMet();
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
		if ( ignoreSchema ) {
			return;
		}
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
	public void createDocumentWork(String indexName, StubDocumentWork work) {
		DocumentWorkCreateCall call = new DocumentWorkCreateCall( indexName, work );
		CallQueue<DocumentWorkCreateCall> callQueue = getDocumentWorkCreateCalls( call.documentKey() );
		callQueue.verify( call, DocumentWorkCreateCall::verify, noExpectationsBehavior( () -> null ) );
	}

	@Override
	public void discardDocumentWork(String indexName, StubDocumentWork work) {
		DocumentWorkDiscardCall call = new DocumentWorkDiscardCall( indexName, work );
		CallQueue<DocumentWorkDiscardCall> callQueue = getDocumentWorkDiscardCalls( call.documentKey() );
		callQueue.verify( call, DocumentWorkDiscardCall::verify, noExpectationsBehavior( () -> null ) );
	}

	@Override
	public CompletableFuture<?> executeDocumentWork(String indexName, StubDocumentWork work) {
		DocumentWorkExecuteCall call = new DocumentWorkExecuteCall( indexName, work );
		CallQueue<DocumentWorkExecuteCall> callQueue = getDocumentWorkExecuteCalls( call.documentKey() );
		return callQueue.verify( call, DocumentWorkExecuteCall::verify,
				noExpectationsBehavior( () -> CompletableFuture.completedFuture( null ) ) );
	}

	@Override
	public <T> SearchResult<T> executeSearchWork(Set<String> indexNames, StubSearchWork work,
			StubSearchProjectionContext projectionContext, LoadingContext<?, ?> loadingContext,
			StubSearchProjection<T> rootProjection, Deadline deadline) {
		return searchCalls.verify(
				new SearchWorkCall<>( indexNames, work, projectionContext, loadingContext, rootProjection,
						deadline ),
				(call1, call2) -> call1.verify( call2 ),
				noExpectationsBehavior( () -> new SimpleSearchResult<>( SimpleSearchResultTotal.exact( 0L ),
						Collections.emptyList(), Collections.emptyMap(), Duration.ZERO, false ) )
		);
	}

	@Override
	public CompletableFuture<?> executeIndexScaleWork(String indexName, StubIndexScaleWork work) {
		return getIndexScaleWorkCalls( indexName )
				.verify(
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

	@Override
	public <T> SearchScroll<T> executeScrollWork(Set<String> indexNames, StubSearchWork work, int chunkSize,
			StubSearchProjectionContext projectionContext, LoadingContext<?, ?> loadingContext,
			StubSearchProjection<T> rootProjection, TimingSource timingSource) {
		return scrollCalls.verify(
				new ScrollWorkCall<>( indexNames, work, chunkSize, this, projectionContext, loadingContext,
						rootProjection, timingSource ),
				(call1, call2) -> call1.verify( call2 ),
				noExpectationsBehavior( () -> new StubSearchScroll<>(
						this, indexNames, null, null, null, null, null
				) )
		);
	}

	@Override
	public void executeCloseScrollWork(Set<String> indexNames) {
		closeScrollCalls.verify(
				new CloseScrollWorkCall( indexNames ),
				CloseScrollWorkCall::verify,
				noExpectationsBehavior( () -> null )
		);
	}

	@Override
	public <T> SearchScrollResult<T> executeNextScrollWork(Set<String> indexNames, StubSearchWork work,
			StubSearchProjectionContext projectionContext, LoadingContext<?, ?> loadingContext,
			StubSearchProjection<T> rootProjection, Deadline deadline) {
		return nextScrollCalls.verify(
				new NextScrollWorkCall<>( indexNames, work, projectionContext, loadingContext, rootProjection,
						deadline ),
				(call1, call2) -> call1.verify( call2 ),
				noExpectationsBehavior( () ->
						new SimpleSearchScrollResult<>( SimpleSearchResultTotal.exact( 0L ),
								false, Collections.emptyList(), Duration.ZERO, false ) )
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
			fail( "No call expected, but got: " + call );
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
