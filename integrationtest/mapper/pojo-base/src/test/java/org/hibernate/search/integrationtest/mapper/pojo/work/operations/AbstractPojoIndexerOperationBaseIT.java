/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.work.operations;

import static org.hibernate.search.integrationtest.mapper.pojo.work.operations.BackendIndexingOperation.addWorkInfo;
import static org.hibernate.search.util.impl.test.FutureAssert.assertThatFuture;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.hibernate.search.mapper.pojo.route.DocumentRouteDescriptor;
import org.hibernate.search.mapper.pojo.route.DocumentRoutesDescriptor;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.mapper.pojo.standalone.work.SearchIndexer;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Test;

/**
 * Tests of individual operations in {@link org.hibernate.search.mapper.pojo.work.spi.PojoIndexer}.
 */
public abstract class AbstractPojoIndexerOperationBaseIT extends AbstractPojoIndexingOperationIT {

	@Test
	public void success() {
		CompletableFuture<?> futureFromBackend = new CompletableFuture<>();
		try ( SearchSession session = createSession() ) {
			SearchIndexer indexer = session.indexer();

			expectOperation( futureFromBackend, 1, null, "1" );
			CompletionStage<?> returnedFuture = scenario().execute( indexer, null, IndexedEntity.of( 1 ) );
			backendMock.verifyExpectationsMet();
			assertThatFuture( returnedFuture ).isPending();

			futureFromBackend.complete( null );
			assertThatFuture( returnedFuture ).isSuccessful();
		}
	}

	@Test
	public void providedId() {
		CompletableFuture<?> futureFromBackend = new CompletableFuture<>();
		try ( SearchSession session = createSession() ) {
			SearchIndexer indexer = session.indexer();

			expectOperation( futureFromBackend, 42, null, "1" );
			CompletionStage<?> returnedFuture = scenario().execute( indexer, 42, IndexedEntity.of( 1 ) );
			backendMock.verifyExpectationsMet();
			assertThatFuture( returnedFuture ).isPending();

			futureFromBackend.complete( null );
			assertThatFuture( returnedFuture ).isSuccessful();
		}
	}

	@Test
	public void providedId_providedRoutes_currentAndNoPrevious() {
		CompletableFuture<?> futureFromBackend = new CompletableFuture<>();
		try ( SearchSession session = createSession() ) {
			SearchIndexer indexer = session.indexer();

			// Since we don't provide any previous routes, we don't expect additional deletes.
			expectOperation( futureFromBackend,
					worksBeforeInSamePlan -> {
						if ( !isAdd() ) {
							// For operations other than add,
							// expect a delete for the previous routes (if different).
							if ( isImplicitRoutingEnabled() ) {
								// If implicit routing is enabled, the provided current route
								// is assumed out-of-date and turned into a previous route.
								worksBeforeInSamePlan
										.delete( b -> addWorkInfo( b, tenantId, "42", "UE-123" ) );

								// If implicit routing is enabled, previous routes are also taken from the routing bridge.
								MyRoutingBridge.previousValues = Arrays.asList( "1", "foo", "3" );
								worksBeforeInSamePlan
										// "1" is ignored as it's the current value
										.delete( b -> addWorkInfo( b, tenantId, "42",
												MyRoutingBridge.toRoutingKey( tenantId, 42, "foo" ) ) )
										.delete( b -> addWorkInfo( b, tenantId, "42",
												MyRoutingBridge.toRoutingKey( tenantId, 42, "3" ) ) );
							}
							// else: if implicit routing is disabled,
							// since we don't provide any previous routes, we don't expect additional deletes.
						}
					},
					42, "UE-123", "1" );
			CompletionStage<?> returnedFuture = scenario().execute( indexer, 42,
					DocumentRoutesDescriptor.of( DocumentRouteDescriptor.of( "UE-123" ), Collections.emptyList() ),
					IndexedEntity.of( 1 ) );
			backendMock.verifyExpectationsMet();
			assertThatFuture( returnedFuture ).isPending();

			futureFromBackend.complete( null );
			assertThatFuture( returnedFuture ).isSuccessful();
		}
	}

	@Test
	public void providedId_providedRoutes_currentAndPrevious() {
		CompletableFuture<?> futureFromBackend = new CompletableFuture<>();
		try ( SearchSession session = createSession() ) {
			SearchIndexer indexer = session.indexer();

			// Since we don't provide any previous routes, we don't expect additional deletes.
			expectOperation( futureFromBackend,
					worksBefore -> {
						if ( !isAdd() ) {
							// For operations other than add, expect a delete for the previous routes (if different).
							worksBefore
									.delete( b -> addWorkInfo( b, tenantId, "42", "UE-121" ) )
									.delete( b -> addWorkInfo( b, tenantId, "42", "UE-122" ) );
							if ( isImplicitRoutingEnabled() ) {
								// If implicit routing is enabled, the provided current route
								// is assumed out-of-date and turned into a previous route.
								worksBefore
										.delete( b -> addWorkInfo( b, tenantId, "42", "UE-123" ) );

								// If implicit routing is enabled, previous routes are also taken from the routing bridge.
								MyRoutingBridge.previousValues = Arrays.asList( "1", "foo", "3" );
								worksBefore
										// "1" is ignored as it's the current value
										.delete( b -> addWorkInfo( b, tenantId, "42",
												MyRoutingBridge.toRoutingKey( tenantId, 42, "foo" ) ) )
										.delete( b -> addWorkInfo( b, tenantId, "42",
												MyRoutingBridge.toRoutingKey( tenantId, 42, "3" ) ) );
							}
						}
					},
					// And only then, expect the actual operation.
					42, "UE-123", "1" );
			CompletionStage<?> returnedFuture = scenario().execute( indexer, 42,
					DocumentRoutesDescriptor.of( DocumentRouteDescriptor.of( "UE-123" ),
							Arrays.asList( DocumentRouteDescriptor.of( "UE-121" ),
									DocumentRouteDescriptor.of( "UE-122" ),
									DocumentRouteDescriptor.of( "UE-123" ) ) ),
					IndexedEntity.of( 1 ) );
			backendMock.verifyExpectationsMet();
			assertThatFuture( returnedFuture ).isPending();

			futureFromBackend.complete( null );
			assertThatFuture( returnedFuture ).isSuccessful();
		}
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3108")
	public void previouslyIndexedWithDifferentRoute() {
		assumeImplicitRoutingEnabled();

		CompletableFuture<?> futureFromBackend = new CompletableFuture<>();
		try ( SearchSession session = createSession() ) {
			SearchIndexer indexer = session.indexer();

			MyRoutingBridge.previousValues = Arrays.asList( "foo" );
			expectOperation( futureFromBackend,
					worksBefore -> {
						if ( !isAdd() ) {
							// For operations other than add, expect a delete for the previous route.
							worksBefore
									.delete( b -> addWorkInfo( b, tenantId, "1",
											MyRoutingBridge.toRoutingKey( tenantId, 1, "foo" ) ) );
						}
					},
					// And only then, expect the actual operation.
					1, null, "1"
			);
			CompletionStage<?> returnedFuture = scenario().execute( indexer, null, IndexedEntity.of( 1 ) );
			backendMock.verifyExpectationsMet();
			assertThatFuture( returnedFuture ).isPending();

			futureFromBackend.complete( null );
			assertThatFuture( returnedFuture ).isSuccessful();
		}
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3108")
	public void previouslyIndexedWithMultipleRoutes() {
		assumeImplicitRoutingEnabled();

		CompletableFuture<?> futureFromBackend = new CompletableFuture<>();
		try ( SearchSession session = createSession() ) {
			SearchIndexer indexer = session.indexer();

			MyRoutingBridge.previousValues = Arrays.asList( "1", "foo", "3" );
			expectOperation( futureFromBackend,
					worksBefore -> {
						if ( !isAdd() ) {
							// For operations other than add, expect a delete for every previous route distinct from the current one.
							backendMock.expectWorks( IndexedEntity.INDEX, commitStrategy, refreshStrategy )
									.delete( b -> addWorkInfo( b, tenantId, "1",
											MyRoutingBridge
													.toRoutingKey( tenantId, 1, "foo" ) ) );
							backendMock.expectWorks( IndexedEntity.INDEX, commitStrategy, refreshStrategy )
									.delete( b -> addWorkInfo( b, tenantId, "1",
											MyRoutingBridge
													.toRoutingKey( tenantId, 1, "3" ) ) );
						}
					},
					// And only then, expect the actual operation.
					1, null, "1"
			);
			CompletionStage<?> returnedFuture = scenario().execute( indexer, null, IndexedEntity.of( 1 ) );
			backendMock.verifyExpectationsMet();
			assertThatFuture( returnedFuture ).isPending();

			futureFromBackend.complete( null );
			assertThatFuture( returnedFuture ).isSuccessful();
		}
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3108")
	public void notIndexed_notPreviouslyIndexed() {
		assumeImplicitRoutingEnabled();

		try ( SearchSession session = createSession() ) {
			SearchIndexer indexer = session.indexer();

			MyRoutingBridge.indexed = false;
			MyRoutingBridge.previouslyIndexed = false;
			// We don't expect the actual operation, which should be skipped because the entity is not indexed.
			CompletionStage<?> returnedFuture = scenario().execute( indexer, null, IndexedEntity.of( 1 ) );
			backendMock.verifyExpectationsMet();
			assertThatFuture( returnedFuture ).isSuccessful();
		}
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3108")
	public void notIndexed_previouslyIndexedWithDifferentRoute() {
		assumeImplicitRoutingEnabled();

		try ( SearchSession session = createSession() ) {
			SearchIndexer indexer = session.indexer();

			MyRoutingBridge.indexed = false;
			MyRoutingBridge.previouslyIndexed = true;
			if ( !isAdd() ) {
				// For operations other than add, expect a delete for the previous route.
				backendMock.expectWorks( IndexedEntity.INDEX, commitStrategy, refreshStrategy )
						.delete( b -> addWorkInfo( b, tenantId, "1",
								MyRoutingBridge.toRoutingKey( tenantId, 1, "1" ) ) );
			}
			// However, we don't expect the actual operation, which should be skipped because the entity is not indexed.
			CompletionStage<?> returnedFuture = scenario().execute( indexer, null, IndexedEntity.of( 1 ) );
			backendMock.verifyExpectationsMet();
			assertThatFuture( returnedFuture ).isSuccessful();
		}
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3108")
	public void notIndexed_previouslyIndexedWithMultipleRoutes() {
		assumeImplicitRoutingEnabled();

		try ( SearchSession session = createSession() ) {
			SearchIndexer indexer = session.indexer();

			MyRoutingBridge.indexed = false;
			MyRoutingBridge.previouslyIndexed = true;
			MyRoutingBridge.previousValues = Arrays.asList( "1", "foo", "3" );
			if ( !isAdd() ) {
				// For operations other than add, expect a delete for every previous route.
				backendMock.expectWorks( IndexedEntity.INDEX, commitStrategy, refreshStrategy )
						.delete( b -> addWorkInfo( b, tenantId, "1",
								MyRoutingBridge.toRoutingKey( tenantId, 1, "1" ) ) );
				backendMock.expectWorks( IndexedEntity.INDEX, commitStrategy, refreshStrategy )
						.delete( b -> addWorkInfo( b, tenantId, "1",
								MyRoutingBridge.toRoutingKey( tenantId, 1, "foo" ) ) );
				backendMock.expectWorks( IndexedEntity.INDEX, commitStrategy, refreshStrategy )
						.delete( b -> addWorkInfo( b, tenantId, "1",
								MyRoutingBridge.toRoutingKey( tenantId, 1, "3" ) ) );
			}
			// However, we don't expect the actual operation, which should be skipped because the entity is not indexed.
			CompletionStage<?> returnedFuture = scenario().execute( indexer, null, IndexedEntity.of( 1 ) );
			backendMock.verifyExpectationsMet();
			assertThatFuture( returnedFuture ).isSuccessful();
		}
	}

	@Test
	public void runtimeException() {
		CompletableFuture<?> futureFromBackend = new CompletableFuture<>();
		RuntimeException exception = new RuntimeException();
		try ( SearchSession session = createSession() ) {
			SearchIndexer indexer = session.indexer();

			expectOperation( futureFromBackend, 1, null, "1" );
			CompletionStage<?> returnedFuture = scenario().execute( indexer, null, IndexedEntity.of( 1 ) );
			backendMock.verifyExpectationsMet();
			assertThatFuture( returnedFuture ).isPending();

			futureFromBackend.completeExceptionally( exception );
			assertThatFuture( returnedFuture ).isFailed( exception );
		}
	}

	@Test
	public void error() {
		CompletableFuture<?> futureFromBackend = new CompletableFuture<>();
		Error error = new Error();
		try ( SearchSession session = createSession() ) {
			SearchIndexer indexer = session.indexer();

			expectOperation( futureFromBackend, 1, null, "1" );
			CompletionStage<?> returnedFuture = scenario().execute( indexer, null, IndexedEntity.of( 1 ) );
			backendMock.verifyExpectationsMet();
			assertThatFuture( returnedFuture ).isPending();

			futureFromBackend.completeExceptionally( error );
			assertThatFuture( returnedFuture ).isFailed( error );
		}
	}

	@Override
	protected boolean isImplicitRoutingEnabled() {
		return routingBinder != null;
	}
}
