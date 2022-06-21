/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.work.operations;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.integrationtest.mapper.pojo.work.operations.BackendIndexingOperation.addWorkInfo;
import static org.junit.Assume.assumeTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.mapper.pojo.standalone.work.SearchIndexingPlan;
import org.hibernate.search.mapper.pojo.route.DocumentRouteDescriptor;
import org.hibernate.search.mapper.pojo.route.DocumentRoutesDescriptor;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Test;

/**
 * Tests of individual operations in {@link org.hibernate.search.mapper.pojo.work.spi.PojoIndexingPlan}
 * when the entity passed to the operation is null.
 */
@TestForIssue(jiraKey = "HSEARCH-4153")
public abstract class AbstractPojoIndexingPlanOperationNullEntityIT extends AbstractPojoIndexingOperationIT {

	@Test
	public void simple() {
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

	@Test
	public void loadingDoesNotFindEntity() {
		assumeTrue(
				"This test only makes sense when "
						+ "the operation is automatically skipped when the entity is absent upon implicit loading",
				scenario().expectSkipOnEntityAbsentAfterImplicitLoading()
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

	@Test
	public void nullProvidedId() {
		try ( SearchSession session = createSession() ) {
			SearchIndexingPlan indexingPlan = session.indexingPlan();
			assertThatThrownBy( () -> scenario().addWithoutInstanceTo( indexingPlan, IndexedEntity.class, null, null ) )
					.isInstanceOf( SearchException.class )
					.hasMessageContainingAll( "Invalid indexing request",
							"if the entity is null, the identifier must be provided explicitly" );
		}
	}

	@Test
	public void providedId_providedRoutes_currentAndNoPrevious() {
		CompletableFuture<?> futureFromBackend = new CompletableFuture<>();
		try ( SearchSession session = createSession() ) {
			SearchIndexingPlan indexingPlan = session.indexingPlan();

			expectIndexedEntityLoadingIfRelevant( Collections.singletonList( 42 ), Collections.singletonList( IndexedEntity.of( 1 ) ) );
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
										.delete( b -> addWorkInfo( b, tenantId, "42", MyRoutingBridge.toRoutingKey( tenantId, 42, "foo" ) ) )
										.delete( b -> addWorkInfo( b, tenantId, "42", MyRoutingBridge.toRoutingKey( tenantId, 42, "3" ) ) );
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

	@Test
	public void providedId_providedRoutes_currentAndPrevious() {
		CompletableFuture<?> futureFromBackend = new CompletableFuture<>();
		try ( SearchSession session = createSession() ) {
			SearchIndexingPlan indexingPlan = session.indexingPlan();

			expectIndexedEntityLoadingIfRelevant( Collections.singletonList( 42 ), Collections.singletonList( IndexedEntity.of( 1 ) ) );
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
										.delete( b -> addWorkInfo( b, tenantId, "42", MyRoutingBridge.toRoutingKey( tenantId, 42, "foo" ) ) )
										.delete( b -> addWorkInfo( b, tenantId, "42", MyRoutingBridge.toRoutingKey( tenantId, 42, "3" ) ) );
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

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3108")
	public void previouslyIndexedWithDifferentRoute() {
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

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3108")
	public void previouslyIndexedWithMultipleRoutes() {
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

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3108")
	public void notIndexed_notPreviouslyIndexed() {
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

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3108")
	public void notIndexed_previouslyIndexedWithDifferentRoute() {
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

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3108")
	public void notIndexed_previouslyIndexedWithMultipleRoutes() {
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
