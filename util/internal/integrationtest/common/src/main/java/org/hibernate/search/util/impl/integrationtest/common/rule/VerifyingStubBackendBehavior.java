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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.hibernate.search.engine.backend.spi.BackendBuildContext;
import org.hibernate.search.engine.common.timing.Deadline;
import org.hibernate.search.engine.common.timing.spi.TimingSource;
import org.hibernate.search.engine.reporting.spi.ContextualFailureCollector;
import org.hibernate.search.engine.search.loading.spi.SearchLoadingContext;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.engine.search.query.SearchScroll;
import org.hibernate.search.engine.search.query.SearchScrollResult;
import org.hibernate.search.engine.search.query.spi.SimpleSearchResult;
import org.hibernate.search.engine.search.query.spi.SimpleSearchResultTotal;
import org.hibernate.search.engine.search.query.spi.SimpleSearchScrollResult;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.impl.ToStringStyle;
import org.hibernate.search.util.common.impl.ToStringTreeBuilder;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.BackendMappingHandle;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.StubBackendBehavior;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.impl.StubIndexModel;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.StubDocumentWork;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.StubIndexScaleWork;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.StubSchemaManagementWork;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl.StubSearchProjection;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl.StubSearchProjectionContext;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.query.impl.StubSearchScroll;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.query.impl.StubSearchWork;

class VerifyingStubBackendBehavior extends StubBackendBehavior {

	private final Supplier<BackendIndexingWorkExpectations> indexingWorkExpectationsSupplier;
	private final List<CompletionStage<BackendMappingHandle>> mappingHandlePromises = new ArrayList<>();

	private final CallQueue.Settings indexingCallQueueSettings;
	private final CallQueue.Settings nonIndexingCallQueueSettings;

	private final Map<IndexFieldKey, CallBehavior<Void>> indexFieldAddBehaviors = new ConcurrentHashMap<>();

	private final List<ParameterizedCallBehavior<BackendBuildContext, Void>> createBackendBehaviors = Collections.synchronizedList( new ArrayList<>() );

	private final List<CallBehavior<Void>> stopBackendBehaviors = Collections.synchronizedList( new ArrayList<>() );

	private final Map<String, CallQueue<IndexScaleWorkCall>> indexScaleWorkCalls = new ConcurrentHashMap<>();

	private final Map<String, CallQueue<SchemaDefinitionCall>> schemaDefinitionCalls = new ConcurrentHashMap<>();

	private final Map<String, CallQueue<SchemaManagementWorkCall>> schemaManagementWorkCall = new ConcurrentHashMap<>();

	private final Map<DocumentKey, CallQueue<DocumentWorkCreateCall>> documentWorkCreateCalls = new ConcurrentHashMap<>();
	private final Map<DocumentKey, CallQueue<DocumentWorkDiscardCall>> documentWorkDiscardCalls = new ConcurrentHashMap<>();
	private final Map<DocumentKey, CallQueue<DocumentWorkExecuteCall>> documentWorkExecuteCalls = new ConcurrentHashMap<>();

	private final CallQueue<SearchWorkCall<?>> searchCalls;
	private final CallQueue<CountWorkCall> countCalls;
	private final CallQueue<ScrollWorkCall<?>> scrollCalls;
	private final CallQueue<CloseScrollWorkCall> closeScrollCalls;
	private final CallQueue<NextScrollWorkCall<?>> nextScrollCalls;

	private volatile boolean lenient = false;

	private volatile boolean ignoreSchema = false;

	VerifyingStubBackendBehavior(Supplier<BackendIndexingWorkExpectations> indexingWorkExpectationsSupplier) {
		this.indexingWorkExpectationsSupplier = indexingWorkExpectationsSupplier;
		indexingCallQueueSettings = new CallQueue.Settings() {
			@Override
			public boolean allowDuplicates() {
				return indexingWorkExpectationsSupplier.get().allowDuplicateIndexing();
			}
		};
		nonIndexingCallQueueSettings = new CallQueue.Settings() {
			@Override
			public boolean allowDuplicates() {
				return false;
			}
		};
		this.searchCalls = new CallQueue<>( nonIndexingCallQueueSettings );
		this.countCalls = new CallQueue<>( nonIndexingCallQueueSettings );
		this.scrollCalls = new CallQueue<>( nonIndexingCallQueueSettings );
		this.closeScrollCalls = new CallQueue<>( nonIndexingCallQueueSettings );
		this.nextScrollCalls = new CallQueue<>( nonIndexingCallQueueSettings );
	}

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
		return schemaDefinitionCalls.computeIfAbsent( indexName,
				ignored -> new CallQueue<>( nonIndexingCallQueueSettings ) );
	}

	CallQueue<SchemaManagementWorkCall> getSchemaManagementWorkCalls(String indexName) {
		return schemaManagementWorkCall.computeIfAbsent( indexName,
				ignored -> new CallQueue<>( nonIndexingCallQueueSettings ) );
	}

	CallQueue<DocumentWorkCreateCall> getDocumentWorkCreateCalls(DocumentKey documentKey) {
		return documentWorkCreateCalls.computeIfAbsent( documentKey,
				ignored -> new CallQueue<>( indexingCallQueueSettings ) );
	}

	CallQueue<DocumentWorkDiscardCall> getDocumentWorkDiscardCalls(DocumentKey documentKey) {
		return documentWorkDiscardCalls.computeIfAbsent( documentKey,
				ignored -> new CallQueue<>( indexingCallQueueSettings ) );
	}

	Map<DocumentKey, CallQueue<DocumentWorkExecuteCall>> getDocumentWorkExecuteCalls() {
		return documentWorkExecuteCalls;
	}

	CallQueue<DocumentWorkExecuteCall> getDocumentWorkExecuteCalls(DocumentKey documentKey) {
		return documentWorkExecuteCalls.computeIfAbsent( documentKey,
				ignored -> new CallQueue<>( indexingCallQueueSettings ) );
	}

	CallQueue<IndexScaleWorkCall> getIndexScaleWorkCalls(String indexName) {
		return indexScaleWorkCalls.computeIfAbsent( indexName,
				ignored -> new CallQueue<>( nonIndexingCallQueueSettings ) );
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

	public void resetBackends() {
		mappingHandlePromises.clear();
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
		BackendIndexingWorkExpectations indexingWorkExpectations = indexingWorkExpectationsSupplier.get();

		// The Closer we make sure that all lines in the try-with-resources block below are executed,
		// even if one of the assertions fails;
		// the first AssertionError is thrown at the end of the block,
		// with additional AssertionErrors added as suppressed exceptions.
		// That way, when multiple expectations are not met,
		// we also report the additional ones as suppressed exceptions.
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			// We don't check anything for the various behaviors (createBackendBehaviors, ...): they are ignored if they are not executed.

			// First, we check that there weren't any unexpected calls:
			// those are the most useful to debug problems.
			closer.pushAll( CallQueue::verifyNoUnexpectedCall, schemaDefinitionCalls.values() );
			closer.pushAll( CallQueue::verifyNoUnexpectedCall, indexScaleWorkCalls.values() );
			closer.pushAll( CallQueue::verifyNoUnexpectedCall, schemaManagementWorkCall.values() );
			closer.pushAll( CallQueue::verifyNoUnexpectedCall, documentWorkCreateCalls.values() );
			closer.pushAll( CallQueue::verifyNoUnexpectedCall, documentWorkDiscardCalls.values() );
			closer.pushAll( CallQueue::verifyNoUnexpectedCall, documentWorkExecuteCalls.values() );
			closer.pushAll( CallQueue::verifyNoUnexpectedCall,
					searchCalls, countCalls,
					scrollCalls, closeScrollCalls, nextScrollCalls );

			// Then, we check that whatever we *were* expecting actually happened.
			closer.pushAll( CallQueue::verifyExpectationsMet, schemaDefinitionCalls.values() );
			closer.pushAll( CallQueue::verifyExpectationsMet, indexScaleWorkCalls.values() );
			closer.pushAll( CallQueue::verifyExpectationsMet, schemaManagementWorkCall.values() );
			// For async indexing, allow a slight delay for indexing assertions to be met.
			closer.push(
					expectations -> expectations.awaitIndexingAssertions( () -> {
						try ( Closer<RuntimeException> indexingCloser = new Closer<>() ) {
							indexingCloser.pushAll( CallQueue::verifyExpectationsMet, documentWorkCreateCalls.values() );
							indexingCloser.pushAll( CallQueue::verifyExpectationsMet, documentWorkDiscardCalls.values() );
							indexingCloser.pushAll( CallQueue::verifyExpectationsMet, documentWorkExecuteCalls.values() );
						}
					} ),
					indexingWorkExpectations
			);
			closer.pushAll( CallQueue::verifyExpectationsMet,
					searchCalls, countCalls,
					scrollCalls, closeScrollCalls, nextScrollCalls );
		}

		if ( indexingWorkExpectations.allowDuplicateIndexing() ) {
			// Wait for async processing to finish, so that all duplicate indexing works
			// are executed *before the test makes any other change* to entities.
			// This is to avoid this scenario:
			// - test executes transaction T1: makes changes, pushes a few change events to a queue
			// - test verifies indexing (waits for indexing to happen)
			// - HSearch processes the first few events, indexes entity A
			// - test resumes
			// - test executes transaction T2:
			//   makes changes to entity B in such a way that it wouldn't normally trigger reindexing of A
			//   (e.g. ReindexOnUpdate.SHALLOW, or asymmetric association updates, ...)
			// - HSearch starts processing the next few events, reindexes entity A... with the new content!
			// - test fails because we never expected reindexing of A with the new content.
			//   We did expect A to be indexed, potentially multiple times, but with its old content from T1.
			indexingWorkExpectations.awaitBackgroundIndexingCompletion( CompletableFuture.allOf(
					backendMappingHandles()
							.map( BackendMappingHandle::backgroundIndexingCompletion )
							.toArray( CompletableFuture[]::new )
			) );
		}
	}

	private Stream<BackendMappingHandle> backendMappingHandles() {
		return mappingHandlePromises.stream()
				.map( CompletionStage::toCompletableFuture )
				.map( future -> {
					if ( !future.isDone() ) {
						throw new IllegalStateException( "Future " + future + " was not completed as expected" );
					}
					return future.getNow( null );
				} )
				// The handle may be null if startup failed
				.filter( Objects::nonNull );
	}

	@Override
	public void onCreateBackend(BackendBuildContext context,
			CompletionStage<BackendMappingHandle> mappingHandlePromise) {
		for ( ParameterizedCallBehavior<BackendBuildContext, Void> behavior : createBackendBehaviors ) {
			behavior.execute( context );
		}
		mappingHandlePromises.add( mappingHandlePromise );
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
	public void defineSchema(String indexName, StubIndexModel indexModel) {
		if ( ignoreSchema ) {
			return;
		}
		getSchemaDefinitionCalls( indexName )
				.verify(
						new SchemaDefinitionCall( indexName, indexModel ),
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
		indexingWorkExpectationsSupplier.get().checkCurrentThread( work );
		DocumentWorkCreateCall call = new DocumentWorkCreateCall( indexName, work );
		CallQueue<DocumentWorkCreateCall> callQueue = getDocumentWorkCreateCalls( call.documentKey() );
		callQueue.verify( call, DocumentWorkCreateCall::verify, noExpectationsBehavior( () -> null ) );
	}

	@Override
	public void discardDocumentWork(String indexName, StubDocumentWork work) {
		indexingWorkExpectationsSupplier.get().checkCurrentThread( work );
		DocumentWorkDiscardCall call = new DocumentWorkDiscardCall( indexName, work );
		CallQueue<DocumentWorkDiscardCall> callQueue = getDocumentWorkDiscardCalls( call.documentKey() );
		callQueue.verify( call, DocumentWorkDiscardCall::verify, noExpectationsBehavior( () -> null ) );
	}

	@Override
	public CompletableFuture<?> executeDocumentWork(String indexName, StubDocumentWork work) {
		indexingWorkExpectationsSupplier.get().checkCurrentThread( work );
		DocumentWorkExecuteCall call = new DocumentWorkExecuteCall( indexName, work );
		CallQueue<DocumentWorkExecuteCall> callQueue = getDocumentWorkExecuteCalls( call.documentKey() );
		return callQueue.verify( call, DocumentWorkExecuteCall::verify,
				noExpectationsBehavior( () -> CompletableFuture.completedFuture( null ) ) );
	}

	@Override
	public <T> SearchResult<T> executeSearchWork(Set<String> indexNames, StubSearchWork work,
			StubSearchProjectionContext projectionContext, SearchLoadingContext<?> loadingContext,
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
			StubSearchProjectionContext projectionContext, SearchLoadingContext<?> loadingContext,
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
			StubSearchProjectionContext projectionContext, SearchLoadingContext<?> loadingContext,
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
			fail( "No call expected, but got: " + call + "; details:\n"
					+ new ToStringTreeBuilder( ToStringStyle.multilineDelimiterStructure() ).value( call ) );
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
