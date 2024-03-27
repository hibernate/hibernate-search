/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.work.operations;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.integrationtest.mapper.pojo.work.operations.BackendIndexingOperation.addWorkInfo;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.mapper.pojo.route.DocumentRouteDescriptor;
import org.hibernate.search.mapper.pojo.route.DocumentRoutesDescriptor;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.mapper.pojo.standalone.work.SearchIndexingPlan;
import org.hibernate.search.mapper.pojo.work.IndexingPlanSynchronizationStrategy;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.test.ExceptionMatcherBuilder;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;
import org.hibernate.search.util.impl.test.extension.ExpectedLog4jLog;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import org.apache.logging.log4j.Level;

/**
 * Tests of individual operations in {@link org.hibernate.search.mapper.pojo.work.spi.PojoIndexingPlan}.
 */
public abstract class AbstractPojoIndexingPlanOperationBaseIT extends AbstractPojoIndexingOperationIT {

	@RegisterExtension
	public ExpectedLog4jLog logged = ExpectedLog4jLog.create();

	@ParameterizedTest(name = "commit: {0}, refresh: {1}, tenantID: {2}, routing: {3}")
	@MethodSource("params")
	void simple(DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy, String tenantId,
			MyRoutingBinder routingBinder, IndexingPlanSynchronizationStrategy strategy) {
		setup( commitStrategy, refreshStrategy, tenantId, routingBinder, strategy );
		CompletableFuture<?> futureFromBackend = new CompletableFuture<>();
		try ( SearchSession session = createSession() ) {
			SearchIndexingPlan indexingPlan = session.indexingPlan();
			expectOperation( futureFromBackend, 1, null, "1" );
			scenario().addTo( indexingPlan, null, IndexedEntity.of( 1 ) );
			// The session will wait for completion of the indexing plan upon closing,
			// so we need to complete it now.
			futureFromBackend.complete( null );
		}
	}

	@ParameterizedTest(name = "commit: {0}, refresh: {1}, tenantID: {2}, routing: {3}")
	@MethodSource("params")
	void providedId(DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy, String tenantId,
			MyRoutingBinder routingBinder, IndexingPlanSynchronizationStrategy strategy) {
		setup( commitStrategy, refreshStrategy, tenantId, routingBinder, strategy );
		CompletableFuture<?> futureFromBackend = new CompletableFuture<>();
		try ( SearchSession session = createSession() ) {
			SearchIndexingPlan indexingPlan = session.indexingPlan();
			expectOperation( futureFromBackend, 42, null, "1" );
			scenario().addTo( indexingPlan, 42, IndexedEntity.of( 1 ) );
			// The session will wait for completion of the indexing plan upon closing,
			// so we need to complete it now.
			futureFromBackend.complete( null );
		}
	}

	@ParameterizedTest(name = "commit: {0}, refresh: {1}, tenantID: {2}, routing: {3}")
	@MethodSource("params")
	void providedId_providedRoutes_currentAndNoPrevious(DocumentCommitStrategy commitStrategy,
			DocumentRefreshStrategy refreshStrategy, String tenantId, MyRoutingBinder routingBinder,
			IndexingPlanSynchronizationStrategy strategy) {
		setup( commitStrategy, refreshStrategy, tenantId, routingBinder, strategy );
		CompletableFuture<?> futureFromBackend = new CompletableFuture<>();
		try ( SearchSession session = createSession() ) {
			SearchIndexingPlan indexingPlan = session.indexingPlan();

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
			scenario().addTo( indexingPlan, 42,
					DocumentRoutesDescriptor.of( DocumentRouteDescriptor.of( "UE-123" ), Collections.emptyList() ),
					IndexedEntity.of( 1 ) );
			// The session will wait for completion of the indexing plan upon closing,
			// so we need to complete it now.
			futureFromBackend.complete( null );
		}
	}

	@ParameterizedTest(name = "commit: {0}, refresh: {1}, tenantID: {2}, routing: {3}")
	@MethodSource("params")
	void providedId_providedRoutes_currentAndPrevious(DocumentCommitStrategy commitStrategy,
			DocumentRefreshStrategy refreshStrategy, String tenantId, MyRoutingBinder routingBinder,
			IndexingPlanSynchronizationStrategy strategy) {
		setup( commitStrategy, refreshStrategy, tenantId, routingBinder, strategy );
		CompletableFuture<?> futureFromBackend = new CompletableFuture<>();
		try ( SearchSession session = createSession() ) {
			SearchIndexingPlan indexingPlan = session.indexingPlan();
			expectOperation(
					futureFromBackend,
					worksBeforeInSamePlan -> {
						if ( !isAdd() ) {
							// For operations other than add,
							// expect a delete for the previous routes (if different).
							worksBeforeInSamePlan
									.delete( b -> addWorkInfo( b, tenantId, "42", "UE-121" ) )
									.delete( b -> addWorkInfo( b, tenantId, "42", "UE-122" ) );
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
						}
					},
					42, "UE-123", "1"
			);
			scenario().addTo( indexingPlan, 42,
					DocumentRoutesDescriptor.of( DocumentRouteDescriptor.of( "UE-123" ),
							Arrays.asList( DocumentRouteDescriptor.of( "UE-121" ),
									DocumentRouteDescriptor.of( "UE-122" ),
									DocumentRouteDescriptor.of( "UE-123" ) ) ),
					IndexedEntity.of( 1 ) );
			// The session will wait for completion of the indexing plan upon closing,
			// so we need to complete it now.
			futureFromBackend.complete( null );
		}
	}

	@ParameterizedTest(name = "commit: {0}, refresh: {1}, tenantID: {2}, routing: {3}")
	@MethodSource("params")
	@TestForIssue(jiraKey = "HSEARCH-4538")
	void providedId_providedRoutes_noCurrentAndPrevious(DocumentCommitStrategy commitStrategy,
			DocumentRefreshStrategy refreshStrategy, String tenantId, MyRoutingBinder routingBinder,
			IndexingPlanSynchronizationStrategy strategy) {
		setup( commitStrategy, refreshStrategy, tenantId, routingBinder, strategy );
		CompletableFuture<?> futureFromBackend = new CompletableFuture<>();
		try ( SearchSession session = createSession() ) {
			SearchIndexingPlan indexingPlan = session.indexingPlan();

			if ( isImplicitRoutingEnabled() ) {
				// If implicit routing is enabled,
				// the current route will be determined by the routing bridge
				// and will override the provided current route.
				// If the current route is non null (and here it will be),
				// we'll end up executing the work.
				expectOperation( futureFromBackend, worksBeforeInSamePlan -> {
					if ( !isAdd() ) {
						// For operations other than add,
						// expect a delete for the previous routes.
						worksBeforeInSamePlan
								.delete( b -> addWorkInfo( b, tenantId, "42", "UE-121" ) )
								.delete( b -> addWorkInfo( b, tenantId, "42", "UE-122" ) )
								.delete( b -> addWorkInfo( b, tenantId, "42", "UE-123" ) );

						// If implicit routing is enabled, the provided current route
						// is assumed out-of-date and turned into a previous route.
						// But here the current route is null, so it won't be used.

						// If implicit routing is enabled, previous routes are also taken from the routing bridge.
						MyRoutingBridge.previousValues = Arrays.asList( "foo", "3" );
						worksBeforeInSamePlan
								.delete( b -> addWorkInfo( b, tenantId, "42",
										MyRoutingBridge.toRoutingKey( tenantId, 42, "foo" ) ) )
								.delete( b -> addWorkInfo( b, tenantId, "42",
										MyRoutingBridge.toRoutingKey( tenantId, 42, "3" ) ) );
					}
				}, 42, MyRoutingBridge.toRoutingKey( tenantId, 1, "1" ), "1" );
			}
			else {
				// If implicit routing is disabled,
				// the provided current route being null means the operation will not get executed.
				if ( !isAdd() ) {
					// BUT for operations other than add,
					// we still expect deletions for the previous routes.
					BackendMock.DocumentWorkCallListContext works =
							backendMock.expectWorks( IndexedEntity.INDEX, commitStrategy, refreshStrategy )
									.createAndExecuteFollowingWorks( futureFromBackend );
					works
							.delete( b -> addWorkInfo( b, tenantId, "42", "UE-121" ) )
							.delete( b -> addWorkInfo( b, tenantId, "42", "UE-122" ) )
							.delete( b -> addWorkInfo( b, tenantId, "42", "UE-123" ) );
				}
			}
			scenario().addTo( indexingPlan, 42,
					DocumentRoutesDescriptor.of( null,
							Arrays.asList( DocumentRouteDescriptor.of( "UE-121" ),
									DocumentRouteDescriptor.of( "UE-122" ),
									DocumentRouteDescriptor.of( "UE-123" ) ) ),
					IndexedEntity.of( 1 ) );
			// The session will wait for completion of the indexing plan upon closing,
			// so we need to complete it now.
			futureFromBackend.complete( null );
		}
	}

	@ParameterizedTest(name = "commit: {0}, refresh: {1}, tenantID: {2}, routing: {3}")
	@MethodSource("params")
	@TestForIssue(jiraKey = "HSEARCH-3108")
	void previouslyIndexedWithDifferentRoute(DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy,
			String tenantId, MyRoutingBinder routingBinder, IndexingPlanSynchronizationStrategy strategy) {
		setup( commitStrategy, refreshStrategy, tenantId, routingBinder, strategy );
		assumeImplicitRoutingEnabled();

		CompletableFuture<?> futureFromBackend = new CompletableFuture<>();
		try ( SearchSession session = createSession() ) {
			SearchIndexingPlan indexingPlan = session.indexingPlan();

			MyRoutingBridge.previousValues = Arrays.asList( "foo" );
			expectOperation(
					futureFromBackend,
					worksBeforeInSamePlan -> {
						if ( !isAdd() ) {
							// For operations other than add, expect a delete for the previous route.
							worksBeforeInSamePlan
									.delete( b -> addWorkInfo( b, tenantId, "1",
											MyRoutingBridge.toRoutingKey( tenantId, 1, "foo" ) ) );
						}
					},
					// And only then, expect the actual operation.
					1, null, "1"
			);
			scenario().addTo( indexingPlan, null, IndexedEntity.of( 1 ) );
			// The session will wait for completion of the indexing plan upon closing,
			// so we need to complete it now.
			futureFromBackend.complete( null );
		}
	}

	@ParameterizedTest(name = "commit: {0}, refresh: {1}, tenantID: {2}, routing: {3}")
	@MethodSource("params")
	@TestForIssue(jiraKey = "HSEARCH-3108")
	void previouslyIndexedWithMultipleRoutes(DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy,
			String tenantId, MyRoutingBinder routingBinder, IndexingPlanSynchronizationStrategy strategy) {
		setup( commitStrategy, refreshStrategy, tenantId, routingBinder, strategy );
		assumeImplicitRoutingEnabled();

		CompletableFuture<?> futureFromBackend = new CompletableFuture<>();
		try ( SearchSession session = createSession() ) {
			SearchIndexingPlan indexingPlan = session.indexingPlan();

			MyRoutingBridge.previousValues = Arrays.asList( "1", "foo", "3" );
			expectOperation(
					futureFromBackend,
					worksBeforeInSamePlan -> {
						if ( !isAdd() ) {
							// For operations other than add, expect a delete for every previous route distinct from the current one.
							worksBeforeInSamePlan
									.delete( b -> addWorkInfo( b, tenantId, "1",
											MyRoutingBridge.toRoutingKey( tenantId, 1, "foo" ) ) )
									.delete( b -> addWorkInfo( b, tenantId, "1",
											MyRoutingBridge.toRoutingKey( tenantId, 1, "3" ) ) );
						}
					},
					// And only then, expect the actual operation.
					1, null, "1"
			);
			scenario().addTo( indexingPlan, null, IndexedEntity.of( 1 ) );
			// The session will wait for completion of the indexing plan upon closing,
			// so we need to complete it now.
			futureFromBackend.complete( null );
		}
	}

	@ParameterizedTest(name = "commit: {0}, refresh: {1}, tenantID: {2}, routing: {3}")
	@MethodSource("params")
	@TestForIssue(jiraKey = "HSEARCH-3108")
	void notIndexed_notPreviouslyIndexed(DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy,
			String tenantId, MyRoutingBinder routingBinder, IndexingPlanSynchronizationStrategy strategy) {
		setup( commitStrategy, refreshStrategy, tenantId, routingBinder, strategy );
		assumeImplicitRoutingEnabled();

		try ( SearchSession session = createSession() ) {
			SearchIndexingPlan indexingPlan = session.indexingPlan();

			MyRoutingBridge.indexed = false;
			MyRoutingBridge.previouslyIndexed = false;
			// We don't expect the actual operation, which should be skipped because the entity is not indexed.
			scenario().addTo( indexingPlan, null, IndexedEntity.of( 1 ) );
		}
	}

	@ParameterizedTest(name = "commit: {0}, refresh: {1}, tenantID: {2}, routing: {3}")
	@MethodSource("params")
	@TestForIssue(jiraKey = "HSEARCH-3108")
	void notIndexed_previouslyIndexedWithDifferentRoute(DocumentCommitStrategy commitStrategy,
			DocumentRefreshStrategy refreshStrategy, String tenantId, MyRoutingBinder routingBinder,
			IndexingPlanSynchronizationStrategy strategy) {
		setup( commitStrategy, refreshStrategy, tenantId, routingBinder, strategy );
		assumeImplicitRoutingEnabled();

		CompletableFuture<?> futureFromBackend = new CompletableFuture<>();
		try ( SearchSession session = createSession() ) {
			SearchIndexingPlan indexingPlan = session.indexingPlan();

			MyRoutingBridge.indexed = false;
			MyRoutingBridge.previouslyIndexed = true;
			if ( !isAdd() ) {
				// For operations other than add, expect a delete for the previous route.
				backendMock.expectWorks( IndexedEntity.INDEX, commitStrategy, refreshStrategy )
						.createAndExecuteFollowingWorks( futureFromBackend )
						.delete( b -> addWorkInfo( b, tenantId, "1",
								MyRoutingBridge.toRoutingKey( tenantId, 1, "1" ) ) );
			}
			// However, we don't expect the actual operation, which should be skipped because the entity is not indexed.
			scenario().addTo( indexingPlan, null, IndexedEntity.of( 1 ) );
			// The session will wait for completion of the indexing plan upon closing,
			// so we need to complete it now.
			futureFromBackend.complete( null );
		}
	}

	@ParameterizedTest(name = "commit: {0}, refresh: {1}, tenantID: {2}, routing: {3}")
	@MethodSource("params")
	@TestForIssue(jiraKey = "HSEARCH-3108")
	void notIndexed_previouslyIndexedWithMultipleRoutes(DocumentCommitStrategy commitStrategy,
			DocumentRefreshStrategy refreshStrategy, String tenantId, MyRoutingBinder routingBinder,
			IndexingPlanSynchronizationStrategy strategy) {
		setup( commitStrategy, refreshStrategy, tenantId, routingBinder, strategy );
		assumeImplicitRoutingEnabled();

		CompletableFuture<?> futureFromBackend = new CompletableFuture<>();
		try ( SearchSession session = createSession() ) {
			SearchIndexingPlan indexingPlan = session.indexingPlan();

			MyRoutingBridge.indexed = false;
			MyRoutingBridge.previouslyIndexed = true;
			MyRoutingBridge.previousValues = Arrays.asList( "1", "foo", "3" );
			if ( !isAdd() ) {
				// For operations other than add, expect a delete for every previous route.
				backendMock.expectWorks( IndexedEntity.INDEX, commitStrategy, refreshStrategy )
						.createAndExecuteFollowingWorks( futureFromBackend )
						.delete( b -> addWorkInfo( b, tenantId, "1",
								MyRoutingBridge.toRoutingKey( tenantId, 1, "1" ) ) )
						.delete( b -> addWorkInfo( b, tenantId, "1",
								MyRoutingBridge.toRoutingKey( tenantId, 1, "foo" ) ) )
						.delete( b -> addWorkInfo( b, tenantId, "1",
								MyRoutingBridge.toRoutingKey( tenantId, 1, "3" ) ) );
			}
			// However, we don't expect the actual operation, which should be skipped because the entity is not indexed.
			scenario().addTo( indexingPlan, null, IndexedEntity.of( 1 ) );
			// The session will wait for completion of the indexing plan upon closing,
			// so we need to complete it now.
			futureFromBackend.complete( null );
		}
	}

	@ParameterizedTest(name = "commit: {0}, refresh: {1}, tenantID: {2}, routing: {3}")
	@MethodSource("params")
	@TestForIssue(jiraKey = "HSEARCH-3108")
	void runtimeException(DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy, String tenantId,
			MyRoutingBinder routingBinder, IndexingPlanSynchronizationStrategy strategy) {
		setup( commitStrategy, refreshStrategy, tenantId, routingBinder, strategy );
		CompletableFuture<?> futureFromBackend = new CompletableFuture<>();
		RuntimeException exception = new RuntimeException();

		Runnable work = () -> {
			try ( SearchSession session = createSession() ) {
				SearchIndexingPlan indexingPlan = session.indexingPlan();
				expectOperation( futureFromBackend, 1, null, "1" );
				scenario().addTo( indexingPlan, null, IndexedEntity.of( 1 ) );
				// The session will wait for completion of the indexing plan upon closing,
				// so we need to complete it now.
				futureFromBackend.completeExceptionally( exception );
			}
		};

		assertExceptionalSituation( work, exception );
	}

	@ParameterizedTest(name = "commit: {0}, refresh: {1}, tenantID: {2}, routing: {3}")
	@MethodSource("params")
	void error(DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy, String tenantId,
			MyRoutingBinder routingBinder, IndexingPlanSynchronizationStrategy strategy) {
		setup( commitStrategy, refreshStrategy, tenantId, routingBinder, strategy );
		CompletableFuture<?> futureFromBackend = new CompletableFuture<>();
		Error error = new Error();

		Runnable work = () -> {
			try ( SearchSession session = createSession() ) {
				SearchIndexingPlan indexingPlan = session.indexingPlan();
				expectOperation( futureFromBackend, 1, null, "1" );
				scenario().addTo( indexingPlan, null, IndexedEntity.of( 1 ) );
				// The session will wait for completion of the indexing plan upon closing,
				// so we need to complete it now.
				futureFromBackend.completeExceptionally( error );
			}
		};

		assertExceptionalSituation( work, error );
	}

	private void assertExceptionalSituation(Runnable work, Throwable exception) {
		if ( isErrorLoggedOnly() ) {
			// we are in async case and error will not be thrown, but rather logged so let's check for that:
			logged.expectEvent(
					Level.ERROR,
					ExceptionMatcherBuilder.isException( exception )
							.build(),
					"Background indexing of entities",
					"Entities that could not be indexed correctly:"
			)
					.once();
			work.run();
		}
		else {
			assertThatThrownBy( () -> work.run() )
					// the exception is going to be wrapped in the SearchException
					.rootCause()
					.isSameAs( exception );
		}
	}

	@Override
	protected boolean isImplicitRoutingEnabled() {
		return routingBinder != null;
	}

	private boolean isErrorLoggedOnly() {
		return DocumentCommitStrategy.NONE.equals( commitStrategy )
				&& DocumentRefreshStrategy.NONE.equals( refreshStrategy );
	}

}
