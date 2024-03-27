/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.work.operations;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.integrationtest.mapper.pojo.work.operations.BackendIndexingOperation.addWorkInfo;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

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
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests of individual operations in {@link org.hibernate.search.mapper.pojo.work.spi.PojoIndexingPlan}
 * when the entity passed to the operation is null.
 */
@TestForIssue(jiraKey = "HSEARCH-4153")
public abstract class AbstractPojoIndexingPlanOperationNullEntityIT extends AbstractPojoIndexingOperationIT {

	@ParameterizedTest(name = "commit: {0}, refresh: {1}, tenantID: {2}, routing: {3}")
	@MethodSource("params")
	void simple(DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy, String tenantId,
			MyRoutingBinder routingBinder, IndexingPlanSynchronizationStrategy strategy) {
		setup( commitStrategy, refreshStrategy, tenantId, routingBinder, strategy );
		CompletableFuture<?> futureFromBackend = new CompletableFuture<>();
		try ( SearchSession session = createSession() ) {
			SearchIndexingPlan indexingPlan = session.indexingPlan();
			expectIndexedEntityLoadingIfRelevant( 1 );
			expectOperation( futureFromBackend, 1, null, "1" );
			scenario().addWithoutInstanceTo( indexingPlan, IndexedEntity.class, 1 );
			// The session will wait for completion of the indexing plan upon closing,
			// so we need to complete it now.
			futureFromBackend.complete( null );
		}
	}

	@ParameterizedTest(name = "commit: {0}, refresh: {1}, tenantID: {2}, routing: {3}")
	@MethodSource("params")
	void loadingDoesNotFindEntity(DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy,
			String tenantId, MyRoutingBinder routingBinder, IndexingPlanSynchronizationStrategy strategy) {
		setup( commitStrategy, refreshStrategy, tenantId, routingBinder, strategy );
		assumeTrue(
				scenario().expectSkipOnEntityAbsentAfterImplicitLoading(),
				"This test only makes sense when "
						+ "the operation is automatically skipped when the entity is absent upon implicit loading"
		);

		CompletableFuture<?> futureFromBackend = new CompletableFuture<>();
		try ( SearchSession session = createSession() ) {
			SearchIndexingPlan indexingPlan = session.indexingPlan();
			expectIndexedEntityLoadingIfRelevant( Collections.singletonList( 1 ), Collections.singletonList( null ) );
			// Expect the operation to be skipped, assuming a delete event will come later.
			scenario().addWithoutInstanceTo( indexingPlan, IndexedEntity.class, 1 );
			// The session will wait for completion of the indexing plan upon closing,
			// so we need to complete it now.
			futureFromBackend.complete( null );
		}
	}

	@ParameterizedTest(name = "commit: {0}, refresh: {1}, tenantID: {2}, routing: {3}")
	@MethodSource("params")
	void nullProvidedId(DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy, String tenantId,
			MyRoutingBinder routingBinder, IndexingPlanSynchronizationStrategy strategy) {
		setup( commitStrategy, refreshStrategy, tenantId, routingBinder, strategy );
		try ( SearchSession session = createSession() ) {
			SearchIndexingPlan indexingPlan = session.indexingPlan();
			assertThatThrownBy( () -> scenario().addWithoutInstanceTo( indexingPlan, IndexedEntity.class, null, null ) )
					.isInstanceOf( SearchException.class )
					.hasMessageContainingAll( "Invalid indexing request",
							"if the entity is null, the identifier must be provided explicitly" );
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

			expectIndexedEntityLoadingIfRelevant( Collections.singletonList( 42 ),
					Collections.singletonList( IndexedEntity.of( 1 ) ) );
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
			scenario().addWithoutInstanceTo( indexingPlan, IndexedEntity.class, 42,
					DocumentRoutesDescriptor.of( DocumentRouteDescriptor.of( "UE-123" ), Collections.emptyList() ) );
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

			expectIndexedEntityLoadingIfRelevant( Collections.singletonList( 42 ),
					Collections.singletonList( IndexedEntity.of( 1 ) ) );
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
			scenario().addWithoutInstanceTo( indexingPlan, IndexedEntity.class, 42,
					DocumentRoutesDescriptor.of( DocumentRouteDescriptor.of( "UE-123" ),
							Arrays.asList( DocumentRouteDescriptor.of( "UE-121" ),
									DocumentRouteDescriptor.of( "UE-122" ),
									DocumentRouteDescriptor.of( "UE-123" ) ) ) );
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

			expectIndexedEntityLoadingIfRelevant( 1 );
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
			scenario().addWithoutInstanceTo( indexingPlan, IndexedEntity.class, 1 );
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

			expectIndexedEntityLoadingIfRelevant( 1 );
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
			scenario().addWithoutInstanceTo( indexingPlan, IndexedEntity.class, 1 );
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

			expectIndexedEntityLoadingIfRelevant( 1 );
			MyRoutingBridge.indexed = false;
			MyRoutingBridge.previouslyIndexed = false;
			// We don't expect the actual operation, which should be skipped because the entity is not indexed.
			scenario().addWithoutInstanceTo( indexingPlan, IndexedEntity.class, 1 );
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

			expectIndexedEntityLoadingIfRelevant( 1 );
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
			scenario().addWithoutInstanceTo( indexingPlan, IndexedEntity.class, 1 );
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

			expectIndexedEntityLoadingIfRelevant( 1 );
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
			scenario().addWithoutInstanceTo( indexingPlan, IndexedEntity.class, 1 );
			// The session will wait for completion of the indexing plan upon closing,
			// so we need to complete it now.
			futureFromBackend.complete( null );
		}
	}

	@Override
	protected boolean isImplicitRoutingEnabled() {
		return routingBinder != null && !isDelete();
	}

}
